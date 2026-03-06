package app.leesh.tratic.analyze.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import app.leesh.tratic.chart.domain.Candle;

public final class AnalysisEngine {
    private static final double EPSILON = 1e-9;
    private static final int TREND_N = 20;
    private static final int TREND_FAST_EMA = 10;
    private static final int TREND_SLOW_EMA = 30;
    private static final int VOL_N = 20;
    private static final int VOL_Z_WINDOW = 30;
    private static final int LOCATION_N = 20;
    private static final int ATR_SHORT = 14;
    private static final int ATR_LONG = 100;
    private static final int PRESSURE_VIEW_EMA = 5;

    private AnalysisEngine() {
    }

    /**
     * 전처리된 캔들(현재 버킷 제외)을 입력받아 4축 분석 결과를 계산한다.
     * 입력 캔들 길이가 부족하면 예외를 발생시킨다.
     */
    public static AnalyzeResult analyze(List<Candle> candles, AnalyzeDirection direction) {
        if (candles.size() < ATR_LONG + 5) {
            throw new IllegalArgumentException("not enough candles to analyze");
        }

        double trendScore = calculateTrendScore(candles);
        VolatilityMetrics volatility = calculateVolatility(candles);
        double locationScore = calculateLocationScore(candles);
        PressureMetrics pressure = calculatePressure(candles);

        return new AnalyzeResult(
                direction,
                trendScore,
                volatility.volatilityScore(),
                volatility.volatilityLabel(),
                locationScore,
                pressure.pressureScore(),
                pressure.pressureRaw(),
                pressure.pressureView());
    }

    private static double calculateTrendScore(List<Candle> candles) {
        List<Double> closes = closes(candles);
        double atrN = atr(candles, TREND_N);
        double atrFloor = Math.max(atrN * 0.1, 1e-6);
        double atrBase = Math.max(atrN, atrFloor);

        List<Double> closeTail = tail(closes, TREND_N);
        double trendLr = linearRegressionSlope(closeTail) / (atrBase + EPSILON);

        double emaFast = ema(closes, TREND_FAST_EMA);
        double emaSlow = ema(closes, TREND_SLOW_EMA);
        double trendMa = (emaFast - emaSlow) / (atrBase + EPSILON);

        double trend = 0.5 * trendLr + 0.5 * trendMa;
        return 100.0 * clamp(trend, -1.0, 1.0);
    }

    private static VolatilityMetrics calculateVolatility(List<Candle> candles) {
        List<Double> ratios = atrToSmaRatios(candles, VOL_N);
        List<Double> ratioTail = tail(ratios, VOL_Z_WINDOW);
        double latest = ratioTail.get(ratioTail.size() - 1);
        double mean = average(ratioTail);
        double std = standardDeviation(ratioTail, mean);
        double z = (latest - mean) / (std + EPSILON);
        double volatilityScore = 100.0 * clamp(z / 3.0, 0.0, 1.0);

        if (candles.size() < ATR_LONG + 1) {
            return new VolatilityMetrics(volatilityScore, VolatilityLabel.UNKNOWN);
        }

        double shortVol = atr(candles, ATR_SHORT);
        double longVol = atr(candles, ATR_LONG);
        double ratio = shortVol / (longVol + EPSILON);

        VolatilityLabel label;
        if (ratio > 1.45) {
            label = VolatilityLabel.HIGH;
        } else if (ratio < 0.65) {
            label = VolatilityLabel.LOW;
        } else {
            label = VolatilityLabel.MID;
        }

        return new VolatilityMetrics(volatilityScore, label);
    }

    private static double calculateLocationScore(List<Candle> candles) {
        List<Candle> tail = tailCandles(candles, LOCATION_N);
        double close = toDouble(tail.get(tail.size() - 1).c());
        double lowest = tail.stream().map(Candle::l).mapToDouble(AnalysisEngine::toDouble).min().orElse(close);
        double highest = tail.stream().map(Candle::h).mapToDouble(AnalysisEngine::toDouble).max().orElse(close);

        return 100.0 * ((close - lowest) / (highest - lowest + EPSILON));
    }

    private static PressureMetrics calculatePressure(List<Candle> candles) {
        List<Double> volumeSeries = candles.stream().map(Candle::v).map(AnalysisEngine::toDouble).toList();
        double volumeEma = ema(volumeSeries, 20);

        List<Double> rawSeries = new ArrayList<>();
        for (Candle candle : candles) {
            double high = toDouble(candle.h());
            double low = toDouble(candle.l());
            double open = toDouble(candle.o());
            double close = toDouble(candle.c());
            double range = high - low + EPSILON;

            double posC = 2.0 * ((close - low) / range) - 1.0;
            double body = (close - open) / range;
            double upperWick = high - Math.max(open, close);
            double lowerWick = Math.min(open, close) - low;
            double wickDiff = (lowerWick - upperWick) / range;

            double raw = 0.6 * posC + 0.3 * body + 0.1 * wickDiff;
            rawSeries.add(raw);
        }

        double latestVolume = volumeSeries.get(volumeSeries.size() - 1);
        double volWeight = clamp(latestVolume / (volumeEma + EPSILON), 0.5, 1.5);
        double pressureRaw = volWeight * rawSeries.get(rawSeries.size() - 1);
        double pressureScore = 100.0 * clamp(pressureRaw, -1.0, 1.0);

        double pressureView = ema(tail(rawSeries, PRESSURE_VIEW_EMA), PRESSURE_VIEW_EMA);

        return new PressureMetrics(pressureScore, pressureRaw, pressureView);
    }

    private static List<Double> atrToSmaRatios(List<Candle> candles, int n) {
        List<Double> ratios = new ArrayList<>();
        for (int i = n + 1; i <= candles.size(); i++) {
            List<Candle> window = candles.subList(0, i);
            double atr = atr(window, n);
            List<Double> closes = closes(window);
            double sma = average(tail(closes, n));
            ratios.add(atr / (sma + EPSILON));
        }
        return ratios;
    }

    private static double atr(List<Candle> candles, int n) {
        if (candles.size() < n + 1) {
            throw new IllegalArgumentException("not enough candles for atr");
        }

        List<Double> tr = new ArrayList<>();
        for (int i = candles.size() - n; i < candles.size(); i++) {
            Candle cur = candles.get(i);
            Candle prev = candles.get(i - 1);
            double high = toDouble(cur.h());
            double low = toDouble(cur.l());
            double prevClose = toDouble(prev.c());

            double trVal = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            tr.add(trVal);
        }
        return average(tr);
    }

    private static List<Double> closes(List<Candle> candles) {
        return candles.stream().map(Candle::c).map(AnalysisEngine::toDouble).toList();
    }

    private static double linearRegressionSlope(List<Double> ys) {
        int n = ys.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumXX = 0.0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = ys.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double numerator = (n * sumXY) - (sumX * sumY);
        double denominator = (n * sumXX) - (sumX * sumX);
        return numerator / (denominator + EPSILON);
    }

    private static double ema(List<Double> series, int period) {
        List<Double> tail = tail(series, period);
        double alpha = 2.0 / (period + 1.0);
        double value = tail.get(0);
        for (int i = 1; i < tail.size(); i++) {
            value = alpha * tail.get(i) + (1 - alpha) * value;
        }
        return value;
    }

    private static double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static double standardDeviation(List<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(v -> {
                    double diff = v - mean;
                    return diff * diff;
                })
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private static double toDouble(BigDecimal value) {
        return value.setScale(8, RoundingMode.HALF_UP).doubleValue();
    }

    private static <T> List<T> tail(List<T> source, int n) {
        int start = Math.max(0, source.size() - n);
        return source.subList(start, source.size());
    }

    private static List<Candle> tailCandles(List<Candle> source, int n) {
        int start = Math.max(0, source.size() - n);
        return source.subList(start, source.size());
    }

    private record VolatilityMetrics(double volatilityScore, VolatilityLabel volatilityLabel) {
    }

    private record PressureMetrics(double pressureScore, double pressureRaw, double pressureView) {
    }
}
