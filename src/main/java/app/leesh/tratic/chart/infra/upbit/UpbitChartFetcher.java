package app.leesh.tratic.chart.infra.upbit;

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
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.UpbitProps;
import app.leesh.tratic.chart.service.ChartFetchRequest;
import app.leesh.tratic.chart.service.ChartFetcher;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@Component
public class UpbitChartFetcher implements ChartFetcher {
    private final UpbitApiClient apiClient;
    private final UpbitRateLimiter rateLimiter;
    private final UpbitCandleResponseMapper mapper;
    private final int maxCandleCountPerRequest;

    public UpbitChartFetcher(UpbitApiClient apiClient, UpbitRateLimiter rateLimiter, UpbitCandleResponseMapper mapper, UpbitProps props) {
        this.apiClient = apiClient;
        this.rateLimiter = rateLimiter;
        this.mapper = mapper;
        this.maxCandleCountPerRequest = props.maxCandleCountPerRequest();
        if (this.maxCandleCountPerRequest <= 0) {
            throw new IllegalArgumentException("max candle count per request must be greater than zero");
        }
    }

    @Override
    public Result<Chart, ChartFetchFailure> fetch(ChartFetchRequest req) {
        int requiredCalls = calculateRequiredCalls(req.count());
        return rateLimiter.acquire(requiredCalls).flatMap(ignored -> fetchCandles(req));
    }

    private Result<Chart, ChartFetchFailure> fetchCandles(ChartFetchRequest req) {
        ChartSignature sig = req.sig();
        TimeResolution timeResolution = sig.timeResolution();
        String market = sig.symbol().value();
        Instant to = req.asOf();
        long remaining = req.count();
        Duration step = timeResolution.toDuration();

        List<Candle> candles = new ArrayList<>();
        while (remaining > 0) {
            long batchCount = Math.min(maxCandleCountPerRequest, remaining);
            Result<UpbitCandleResponse[], ChartFetchFailure> res;
            if (isMinutes(timeResolution)) {
                long unit = parseMinuteUnit(timeResolution);
                res = apiClient.fetchMinuteCandles(unit, market, to.toString(), batchCount);
            } else {
                res = apiClient.fetchDayCandles(market, to.toString(), batchCount);
            }

            Result<List<Candle>, ChartFetchFailure> batchResult = res.map(mapper::toCandles);
            if (batchResult instanceof Result.Ok<List<Candle>, ChartFetchFailure> ok) {
                List<Candle> batch = ok.value();
                candles.addAll(batch);

                if (batch.isEmpty()) {
                    break;
                }

                Instant earliest = batch.get(0).time();
                Instant nextTo = earliest.minus(step);
                if (!nextTo.isBefore(to)) {
                    break;
                }
                to = nextTo;
                remaining -= batch.size();
            } else if (batchResult instanceof Result.Err<List<Candle>, ChartFetchFailure> err) {
                return Result.err(err.error());
            }
        }

        List<Candle> sorted = candles.stream()
                .sorted(Comparator.comparing(Candle::time))
                .toList();
        List<Candle> deduplicated = deduplicateSortedByTime(sorted);
        return Result.ok(Chart.of(sig, CandleSeries.ofSorted(deduplicated)));
    }

    @Override
    public Market market() {
        return Market.UPBIT;
    }

    private static boolean isMinutes(TimeResolution timeResolution) {
        return switch (timeResolution) {
            case D1 -> false;
            default -> true;
        };
    }

    private static long parseMinuteUnit(TimeResolution timeResolution) {
        return switch (timeResolution) {
            case M1 -> 1;
            case M3 -> 3;
            case M5 -> 5;
            case M15 -> 15;
            case M30 -> 30;
            case H1 -> 60;
            case H4 -> 240;
            default -> throw new IllegalArgumentException("unsupported minute resolution: " + timeResolution);
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

    private int calculateRequiredCalls(long candleCount) {
        long required = (candleCount + maxCandleCountPerRequest - 1) / maxCandleCountPerRequest;
        if (required > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) required;
    }
}
