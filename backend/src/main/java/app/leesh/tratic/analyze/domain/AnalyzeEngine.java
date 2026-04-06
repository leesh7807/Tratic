package app.leesh.tratic.analyze.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import app.leesh.tratic.analyze.domain.band.AnalyzeBandSet;
import app.leesh.tratic.analyze.domain.band.AnalyzeBandSpec;
import app.leesh.tratic.chart.domain.Candle;
import app.leesh.tratic.chart.domain.TimeResolution;

@Component
public class AnalyzeEngine {
    private final AnalyzeSpecResolver analyzeSpecResolver;

    public AnalyzeEngine(AnalyzeSpecResolver analyzeSpecResolver) {
        this.analyzeSpecResolver = analyzeSpecResolver;
    }

    public AnalyzeResult analyze(List<Candle> candles, TimeResolution resolution, AnalyzeDirection direction) {
        AnalyzeSpec spec = analyzeSpecResolver.resolve(resolution);
        AnalyzeEngineParams params = spec.engineParams();
        AnalyzeBandSpec bandSpec = spec.bandSpec();
        if (candles.size() < minimumRequiredCandles(params)) {
            throw new IllegalArgumentException("not enough candles to analyze");
        }

        double trendScore = calculateTrendScore(candles, params, bandSpec);
        double volatility = calculateVolatility(candles, params);
        double locationScore = calculateLocationScore(candles, params, bandSpec);
        PressureMetrics pressure = calculatePressure(candles, params, bandSpec);

        return new AnalyzeResult(
                direction,
                trendScore,
                volatility,
                locationScore,
                pressure.pressureScore(),
                pressure.pressureRaw(),
                pressure.pressureView());
    }

    public int minimumRequiredCandles(TimeResolution resolution) {
        return analyzeSpecResolver.resolve(resolution).engineParams().minimumRequiredCandles();
    }

    private static int minimumRequiredCandles(AnalyzeEngineParams params) {
        return params.minimumRequiredCandles();
    }

    private static double calculateTrendScore(List<Candle> candles, AnalyzeEngineParams params, AnalyzeBandSpec bandSpec) {
        List<Double> closes = closes(candles);
        double atrN = atr(candles, params.trendN());
        double atrFloor = Math.max(atrN * params.atrFloorRatio(), params.atrFloorMin());
        double atrBase = Math.max(atrN, atrFloor);

        List<Double> closeTail = tail(closes, params.trendN());
        double trendLr = linearRegressionSlope(closeTail, params.epsilon()) / (atrBase + params.epsilon());

        double emaFast = ema(closes, params.trendFastEma());
        double emaSlow = ema(closes, params.trendSlowEma());
        double trendMa = (emaFast - emaSlow) / (atrBase + params.epsilon());

        double trend = 0.5 * trendLr + 0.5 * trendMa;
        return scaleSignedScore(trend, bandSpec.trend());
    }

    private static double calculateVolatility(List<Candle> candles, AnalyzeEngineParams params) {
        List<Double> ratios = atrToSmaRatios(candles, params.volatilityAtrPeriod(), params.epsilon());
        List<Double> ratioTail = tail(ratios, params.volatilityZWindow());
        double latest = ratioTail.get(ratioTail.size() - 1);
        double mean = average(ratioTail);
        double std = standardDeviation(ratioTail, mean);
        double z = (latest - mean) / (std + params.epsilon());
        return 100.0 * clamp(z / params.volatilityZScoreScale(), 0.0, 1.0);
    }

    private static double calculateLocationScore(List<Candle> candles, AnalyzeEngineParams params,
            AnalyzeBandSpec bandSpec) {
        List<Candle> tail = tailCandles(candles, params.locationWindow());
        double close = toDouble(tail.get(tail.size() - 1).c());
        double lowest = tail.stream().map(Candle::l).mapToDouble(AnalyzeEngine::toDouble).min().orElse(close);
        double highest = tail.stream().map(Candle::h).mapToDouble(AnalyzeEngine::toDouble).max().orElse(close);

        double location = (close - lowest) / (highest - lowest + params.epsilon());
        return scaleUnsignedScore(location, bandSpec.location());
    }

    private static PressureMetrics calculatePressure(List<Candle> candles, AnalyzeEngineParams params,
            AnalyzeBandSpec bandSpec) {
        List<Double> volumeSeries = candles.stream().map(Candle::v).map(AnalyzeEngine::toDouble).toList();
        double volumeEma = ema(volumeSeries, params.pressureVolumeEmaPeriod());

        List<Double> rawSeries = new ArrayList<>();
        for (Candle candle : candles) {
            double high = toDouble(candle.h());
            double low = toDouble(candle.l());
            double open = toDouble(candle.o());
            double close = toDouble(candle.c());
            double range = high - low + params.epsilon();

            double posC = 2.0 * ((close - low) / range) - 1.0;
            double body = (close - open) / range;
            double upperWick = high - Math.max(open, close);
            double lowerWick = Math.min(open, close) - low;
            double wickDiff = (lowerWick - upperWick) / range;

            double raw = params.pressurePosCloseWeight() * posC
                    + params.pressureBodyWeight() * body
                    + params.pressureWickDiffWeight() * wickDiff;
            rawSeries.add(raw);
        }

        double latestVolume = volumeSeries.get(volumeSeries.size() - 1);
        double volWeight = clamp(
                latestVolume / (volumeEma + params.epsilon()),
                params.pressureVolumeWeightMin(),
                params.pressureVolumeWeightMax());
        double pressureRaw = volWeight * rawSeries.get(rawSeries.size() - 1);
        double pressureScore = scaleSignedScore(pressureRaw, bandSpec.pressure());

        double pressureView = ema(tail(rawSeries, params.pressureViewEmaPeriod()), params.pressureViewEmaPeriod());

        return new PressureMetrics(pressureScore, pressureRaw, pressureView);
    }

    private static double scaleSignedScore(double raw, AnalyzeBandSet<?> bandSet) {
        return scale(clamp(raw, -1.0, 1.0), -1.0, 1.0, bandSet.minimumScore(), bandSet.maximumScore());
    }

    private static double scaleUnsignedScore(double raw, AnalyzeBandSet<?> bandSet) {
        return scale(clamp(raw, 0.0, 1.0), 0.0, 1.0, bandSet.minimumScore(), bandSet.maximumScore());
    }

    private static double scale(double value, double sourceMin, double sourceMax, double targetMin, double targetMax) {
        double sourceRange = sourceMax - sourceMin;
        if (sourceRange <= 0.0) {
            throw new IllegalArgumentException("source range must be positive");
        }
        double normalized = (value - sourceMin) / sourceRange;
        return targetMin + normalized * (targetMax - targetMin);
    }

    private static List<Double> atrToSmaRatios(List<Candle> candles, int n, double epsilon) {
        List<Double> ratios = new ArrayList<>();
        for (int i = n + 1; i <= candles.size(); i++) {
            List<Candle> window = candles.subList(0, i);
            double atr = atr(window, n);
            List<Double> closes = closes(window);
            double sma = average(tail(closes, n));
            ratios.add(atr / (sma + epsilon));
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
        return candles.stream().map(Candle::c).map(AnalyzeEngine::toDouble).toList();
    }

    private static double linearRegressionSlope(List<Double> ys, double epsilon) {
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
        return numerator / (denominator + epsilon);
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

    private record PressureMetrics(double pressureScore, double pressureRaw, double pressureView) {
    }
}
