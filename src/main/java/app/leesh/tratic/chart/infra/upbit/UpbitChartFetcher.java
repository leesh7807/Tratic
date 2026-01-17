package app.leesh.tratic.chart.infra.upbit;

import org.springframework.stereotype.Component;

import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.chart.infra.shared.MarketException;
import app.leesh.tratic.chart.service.ChartFetchRequest;
import app.leesh.tratic.chart.service.ChartFetcher;

@Component
public class UpbitChartFetcher implements ChartFetcher {
    private final UpbitApiClient apiClient;
    private final UpbitCandleResponseMapper mapper;

    public UpbitChartFetcher(UpbitApiClient apiClient, UpbitCandleResponseMapper mapper) {
        this.apiClient = apiClient;
        this.mapper = mapper;
    }

    @Override
    public Chart fetch(ChartFetchRequest req) throws MarketException {
        ChartSignature sig = req.sig();
        TimeResolution timeResolution = sig.timeResolution();
        String market = sig.symbol().value();
        String to = req.asOf().toString();
        long count = req.count();

        UpbitCandleResponse[] res;
        if (isMinutes(timeResolution)) {
            long unit = parseMinuteUnit(timeResolution);
            res = apiClient.fetchMinuteCandles(unit, market, to, count);
        } else {
            res = apiClient.fetchDayCandles(market, to, count);
        }
        return mapper.toChart(sig, res);
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
