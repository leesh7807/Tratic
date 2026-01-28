package app.leesh.tratic.chart.infra.upbit;

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
public class UpbitChartFetcher implements ChartFetcher {
    private final UpbitApiClient apiClient;
    private final UpbitCandleResponseMapper mapper;

    public UpbitChartFetcher(UpbitApiClient apiClient, UpbitCandleResponseMapper mapper) {
        this.apiClient = apiClient;
        this.mapper = mapper;
    }

    @Override
    public Result<Chart, ChartFetchFailure> fetch(ChartFetchRequest req) {
        ChartSignature sig = req.sig();
        TimeResolution timeResolution = sig.timeResolution();
        String market = sig.symbol().value();
        String to = req.asOf().toString();
        long count = req.count();

        Result<UpbitCandleResponse[], ChartFetchFailure> res;
        if (isMinutes(timeResolution)) {
            long unit = parseMinuteUnit(timeResolution);
            res = apiClient.fetchMinuteCandles(unit, market, to, count);
        } else {
            res = apiClient.fetchDayCandles(market, to, count);
        }
        return res.map(body -> mapper.toChart(sig, body));
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
}
