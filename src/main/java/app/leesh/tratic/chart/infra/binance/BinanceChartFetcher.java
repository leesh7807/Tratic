package app.leesh.tratic.chart.infra.binance;

import org.springframework.stereotype.Component;

import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.chart.service.ChartFetchRequest;
import app.leesh.tratic.chart.service.ChartFetcher;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@Component
public class BinanceChartFetcher implements ChartFetcher {
    private final BinanceApiClient apiClient;
    private final BinanceCandleResponseMapper mapper;

    public BinanceChartFetcher(BinanceApiClient apiClient, BinanceCandleResponseMapper mapper) {
        this.apiClient = apiClient;
        this.mapper = mapper;
    }

    @Override
    public Result<Chart, ChartFetchFailure> fetch(ChartFetchRequest req) {
        ChartSignature sig = req.sig();
        String symbol = sig.symbol().value();
        String interval = parseTimeResolution(sig.timeResolution());
        long to = req.asOf().toEpochMilli();
        int limit = (int) req.count();

        Result<BinanceCandleResponse[], ChartFetchFailure> res = apiClient.fetchCandlesTo(sig, symbol, interval, to,
                limit);
        return res.map(body -> mapper.toChart(sig, body));
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
}
