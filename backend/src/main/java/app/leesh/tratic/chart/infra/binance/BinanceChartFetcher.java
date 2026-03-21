package app.leesh.tratic.chart.infra.binance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.Instant;

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
        if (req.count() > maxCandlesPerCall) {
            return Result.err(new ChartFetchFailure.InvalidRequest(Market.BINANCE));
        }

        String symbol = sig.symbol().value();
        String interval = parseTimeResolution(sig.timeResolution());
        int limit = (int) req.count();
        Result<BinanceCandleResponse[], ChartFetchFailure> res = apiClient.fetchCandlesTo(sig, symbol, interval,
                req.asOf().toEpochMilli(), limit);

        if (res instanceof Result.Err<BinanceCandleResponse[], ChartFetchFailure> err) {
            return Result.err(err.error());
        }

        List<Candle> candles = mapper.toCandles(((Result.Ok<BinanceCandleResponse[], ChartFetchFailure>) res).value());

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
