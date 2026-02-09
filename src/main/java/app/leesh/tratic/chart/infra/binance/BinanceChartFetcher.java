package app.leesh.tratic.chart.infra.binance;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import app.leesh.tratic.chart.domain.Candle;
import app.leesh.tratic.chart.domain.CandleSeries;
import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.BinanceProps;
import app.leesh.tratic.chart.service.ChartFetchRequest;
import app.leesh.tratic.chart.service.ChartFetcher;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@Component
public class BinanceChartFetcher implements ChartFetcher {
    private final BinanceApiClient apiClient;
    private final BinanceCandleResponseMapper mapper;
    private final int maxCandlesPerCall;

    public BinanceChartFetcher(BinanceApiClient apiClient, BinanceCandleResponseMapper mapper, BinanceProps props) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.maxCandlesPerCall = props.maxCandlesPerCall();
    }

    @Override
    public Result<Chart, ChartFetchFailure> fetch(ChartFetchRequest req) {
        ChartSignature sig = req.sig();
        String symbol = sig.symbol().value();
        String interval = parseTimeResolution(sig.timeResolution());
        Duration step = sig.timeResolution().toDuration();
        long remaining = req.count();
        long to = req.asOf().toEpochMilli();

        List<Candle> candles = new ArrayList<>();
        while (remaining > 0) {
            int limit = (int) Math.min(maxCandlesPerCall, remaining);
            Result<BinanceCandleResponse[], ChartFetchFailure> res = apiClient.fetchCandlesTo(sig, symbol, interval,
                    to, limit);

            BinanceCandleResponse[] body;
            if (res instanceof Result.Ok<BinanceCandleResponse[], ChartFetchFailure> ok) {
                body = ok.value();
            } else {
                var err = (Result.Err<BinanceCandleResponse[], ChartFetchFailure>) res;
                return Result.err(err.error());
            }

            List<Candle> batch = mapper.toCandles(body);
            candles.addAll(batch);

            if (batch.isEmpty()) {
                break;
            }

            Instant earliest = batch.get(0).time();
            to = earliest.minus(step).toEpochMilli();
            remaining -= batch.size();
        }

        List<Candle> sorted = candles.stream()
                .sorted(Comparator.comparing(Candle::time))
                .toList();
        List<Candle> deduplicated = deduplicateSortedByTime(sorted);

        return Result.ok(Chart.of(sig, CandleSeries.ofSorted(deduplicated)));
    }

    @Override
    public Market market() {
        return Market.BINANCE;
    }

    private static String parseTimeResolution(TimeResolution timeResolution) {
        return switch (timeResolution) {
            case M1 -> "1m";
            case M3 -> "3m";
            case M5 -> "5m";
            case M15 -> "15m";
            case M30 -> "30m";
            case H1 -> "1h";
            case H4 -> "4h";
            case D1 -> "1d";
            default -> throw new IllegalArgumentException("unsupported timeframe: " + timeResolution);
        };
    }

    private static List<Candle> deduplicateSortedByTime(List<Candle> sortedCandles) {
        List<Candle> deduplicated = new ArrayList<>(sortedCandles.size());
        Instant lastTime = null;
        for (Candle candle : sortedCandles) {
            if (!candle.time().equals(lastTime)) {
                deduplicated.add(candle);
                lastTime = candle.time();
            }
        }
        return deduplicated;
    }
}
