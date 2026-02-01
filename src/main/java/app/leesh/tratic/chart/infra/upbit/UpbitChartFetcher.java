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

    /**
     * 요청된 캔들 수를 200개 단위로 분할 호출하고 결과를 병합해 차트를 만든다.
     * 응답이 비면 더 이상 데이터가 없다고 보고 종료한다.
     * 비어있는 응답은 거래 없음으로 가정하고 정상으로 간주한다.
     */
    @Override
    public Result<Chart, ChartFetchFailure> fetch(ChartFetchRequest req) {
        ChartSignature sig = req.sig();
        TimeResolution timeResolution = sig.timeResolution();
        String market = sig.symbol().value();
        Instant to = req.asOf();
        long remaining = req.count();
        Duration step = timeResolution.toDuration();

        List<Candle> candles = new ArrayList<>();
        while (remaining > 0) {
            long batchCount = Math.min(200, remaining);
            Result<UpbitCandleResponse[], ChartFetchFailure> res;
            if (isMinutes(timeResolution)) {
                long unit = parseMinuteUnit(timeResolution);
                res = apiClient.fetchMinuteCandles(unit, market, to.toString(), batchCount);
            } else {
                res = apiClient.fetchDayCandles(market, to.toString(), batchCount);
            }

            UpbitCandleResponse[] body;
            if (res instanceof Result.Ok<UpbitCandleResponse[], ChartFetchFailure> ok) {
                body = ok.value();
            } else {
                var err = (Result.Err<UpbitCandleResponse[], ChartFetchFailure>) res;
                return Result.err(err.error());
            }
            List<Candle> batch = mapper.toCandles(body);
            candles.addAll(batch);

            // 응답이 비면 더 이상 데이터가 없다고 판단한다.
            if (batch.isEmpty()) {
                break;
            }

            // 가장 오래된 캔들 시각에서 해상도만큼 뺀 값을 다음 요청의 to로 사용한다.
            Instant earliest = batch.get(0).time();
            to = earliest.minus(step);
            remaining -= batch.size();
        }

        List<Candle> sorted = candles.stream()
                .sorted(Comparator.comparing(Candle::time))
                .toList();
        return Result.ok(Chart.of(sig, CandleSeries.ofSorted(sorted)));
    }

    /**
     * 이 fetcher가 담당하는 마켓을 반환한다.
     */
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
