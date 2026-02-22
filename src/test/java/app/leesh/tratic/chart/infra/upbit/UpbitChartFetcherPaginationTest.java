package app.leesh.tratic.chart.infra.upbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.Symbol;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.UpbitProps;
import app.leesh.tratic.chart.service.ChartFetchRequest;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@ExtendWith(MockitoExtension.class)
public class UpbitChartFetcherPaginationTest {

    @Mock
    private UpbitApiClient apiClient;

    @Test
    @DisplayName("요청 수가 페이지 한도를 넘으면 설정된 크기로 페이지네이션해 순차 호출한다")
    public void fetch_paginatesRequestsByConfiguredChunkSize_and_rollsBackToEarliestMinusResolution() {
        Duration step = Duration.ofMinutes(5);
        int pageLimit = 200;
        int firstCount = 200;
        int secondCount = 200;
        int thirdCount = 50;

        Instant firstEarliest = Instant.parse("2026-01-01T00:00:00Z");
        Instant firstLatest = firstEarliest.plus(step.multipliedBy(firstCount - 1));
        Instant secondEarliest = firstEarliest.minus(step.multipliedBy(secondCount));
        Instant thirdEarliest = secondEarliest.minus(step.multipliedBy(thirdCount));

        UpbitCandleResponse[] batch1 = buildCandles("KRW-BTC", firstEarliest, firstCount, step, 5);
        UpbitCandleResponse[] batch2 = buildCandles("KRW-BTC", secondEarliest, secondCount, step, 5);
        UpbitCandleResponse[] batch3 = buildCandles("KRW-BTC", thirdEarliest, thirdCount, step, 5);

        when(apiClient.fetchMinuteCandles(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn(Result.ok(batch1))
                .thenReturn(Result.ok(batch2))
                .thenReturn(Result.ok(batch3));
        UpbitChartFetcher fetcher = new UpbitChartFetcher(apiClient, new UpbitCandleResponseMapper(),
                upbitProps(pageLimit));
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, firstLatest, firstCount + secondCount + thirdCount);

        Result<Chart, ChartFetchFailure> result = fetcher.fetch(req);

        assertInstanceOf(Result.Ok.class, result);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> countCaptor = ArgumentCaptor.forClass(Long.class);
        verify(apiClient, times(3)).fetchMinuteCandles(eq(5L), eq("KRW-BTC"), toCaptor.capture(),
                countCaptor.capture());
        List<String> toValues = toCaptor.getAllValues();
        List<Long> countValues = countCaptor.getAllValues();

        assertEquals(firstLatest.toString(), toValues.get(0));
        assertEquals(firstCount, countValues.get(0));

        assertEquals(firstEarliest.minus(step).toString(), toValues.get(1));
        assertEquals(secondCount, countValues.get(1));

        assertEquals(secondEarliest.minus(step).toString(), toValues.get(2));
        assertEquals(thirdCount, countValues.get(2));
    }

    @Test
    @DisplayName("중간 페이지 응답이 부분 개수여도 남은 요청 수 기준으로 다음 페이지를 계속 조회한다")
    public void fetch_paginatesWhenResponseIsPartial() {
        Duration step = Duration.ofMinutes(5);
        int pageLimit = 200;
        int firstCount = 200;
        int secondResponseCount = 120;
        int thirdCount = 130;

        Instant firstEarliest = Instant.parse("2026-01-01T00:00:00Z");
        Instant firstLatest = firstEarliest.plus(step.multipliedBy(firstCount - 1));
        Instant secondEarliest = firstEarliest.minus(step.multipliedBy(secondResponseCount));
        Instant thirdEarliest = secondEarliest.minus(step.multipliedBy(thirdCount));

        UpbitCandleResponse[] batch1 = buildCandles("KRW-BTC", firstEarliest, firstCount, step, 5);
        UpbitCandleResponse[] batch2 = buildCandles("KRW-BTC", secondEarliest, secondResponseCount, step, 5);
        UpbitCandleResponse[] batch3 = buildCandles("KRW-BTC", thirdEarliest, thirdCount, step, 5);

        when(apiClient.fetchMinuteCandles(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn(Result.ok(batch1))
                .thenReturn(Result.ok(batch2))
                .thenReturn(Result.ok(batch3));
        UpbitChartFetcher fetcher = new UpbitChartFetcher(apiClient, new UpbitCandleResponseMapper(),
                upbitProps(pageLimit));
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, firstLatest, 450);

        Result<Chart, ChartFetchFailure> result = fetcher.fetch(req);

        assertInstanceOf(Result.Ok.class, result);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> countCaptor = ArgumentCaptor.forClass(Long.class);
        verify(apiClient, times(3)).fetchMinuteCandles(eq(5L), eq("KRW-BTC"), toCaptor.capture(),
                countCaptor.capture());
        List<String> toValues = toCaptor.getAllValues();
        List<Long> countValues = countCaptor.getAllValues();

        assertEquals(firstLatest.toString(), toValues.get(0));
        assertEquals(firstCount, countValues.get(0));

        assertEquals(firstEarliest.minus(step).toString(), toValues.get(1));
        assertEquals(firstCount, countValues.get(1));

        assertEquals(secondEarliest.minus(step).toString(), toValues.get(2));
        assertEquals(thirdCount, countValues.get(2));
    }

    @Test
    @DisplayName("페이지 응답이 비어 있으면 더 이상 과거 데이터가 없다고 보고 조회를 중단한다")
    public void fetch_stopsWhenResponseIsEmpty() {
        Duration step = Duration.ofMinutes(5);
        int pageLimit = 200;
        int firstCount = 200;

        Instant firstEarliest = Instant.parse("2026-01-01T00:00:00Z");
        Instant firstLatest = firstEarliest.plus(step.multipliedBy(firstCount - 1));

        UpbitCandleResponse[] batch1 = buildCandles("KRW-BTC", firstEarliest, firstCount, step, 5);
        UpbitCandleResponse[] empty = new UpbitCandleResponse[0];

        when(apiClient.fetchMinuteCandles(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn(Result.ok(batch1))
                .thenReturn(Result.ok(empty));
        UpbitChartFetcher fetcher = new UpbitChartFetcher(apiClient, new UpbitCandleResponseMapper(),
                upbitProps(pageLimit));
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, firstLatest, 450);

        Result<Chart, ChartFetchFailure> result = fetcher.fetch(req);

        assertInstanceOf(Result.Ok.class, result);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> countCaptor = ArgumentCaptor.forClass(Long.class);
        verify(apiClient, times(2)).fetchMinuteCandles(eq(5L), eq("KRW-BTC"), toCaptor.capture(),
                countCaptor.capture());
        List<String> toValues = toCaptor.getAllValues();
        List<Long> countValues = countCaptor.getAllValues();

        assertEquals(firstLatest.toString(), toValues.get(0));
        assertEquals(firstCount, countValues.get(0));

        assertEquals(firstEarliest.minus(step).toString(), toValues.get(1));
        assertEquals(firstCount, countValues.get(1));
    }

    @Test
    @DisplayName("페이지 간 겹친 캔들은 최종 정렬 이후 시간 기준으로 중복 제거된다")
    public void fetch_deduplicatesOverlappedCandlesAcrossPaginationBatches() {
        Duration step = Duration.ofMinutes(5);
        int pageLimit = 3;

        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t1 = t0.plus(step);
        Instant t2 = t1.plus(step);
        Instant tMinus1 = t0.minus(step);
        Instant tMinus2 = tMinus1.minus(step);

        UpbitCandleResponse[] batch1 = buildCandles("KRW-BTC", t0, 3, step, 5);
        UpbitCandleResponse[] batch2 = new UpbitCandleResponse[] {
                candle("KRW-BTC", tMinus1, 5),
                candle("KRW-BTC", t0, 5),
                candle("KRW-BTC", t1, 5)
        };
        UpbitCandleResponse[] batch3 = new UpbitCandleResponse[] {
                candle("KRW-BTC", tMinus2, 5)
        };

        when(apiClient.fetchMinuteCandles(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn(Result.ok(batch1))
                .thenReturn(Result.ok(batch2))
                .thenReturn(Result.ok(batch3));
        UpbitChartFetcher fetcher = new UpbitChartFetcher(apiClient, new UpbitCandleResponseMapper(),
                upbitProps(pageLimit));
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, t2, 7);

        Result<Chart, ChartFetchFailure> result = fetcher.fetch(req);

        assertInstanceOf(Result.Ok.class, result);
        verify(apiClient, times(3)).fetchMinuteCandles(eq(5L), eq("KRW-BTC"), anyString(), anyLong());
    }

    @Test
    @DisplayName("요청 캔들 수가 즉시 처리 가능한 호출 수를 넘으면 fail-fast로 종료한다")
    public void fetch_failsFastWhenRequiredCallsExceedPerSecondLimit() {
        int pageLimit = 200;
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, Instant.parse("2026-01-01T00:00:00Z"), 2001);

        UpbitChartFetcher fetcher = new UpbitChartFetcher(apiClient, new UpbitCandleResponseMapper(),
                upbitProps(pageLimit));

        Result<Chart, ChartFetchFailure> result = fetcher.fetch(req);

        assertInstanceOf(Result.Err.class, result);
        Result.Err<Chart, ChartFetchFailure> err = (Result.Err<Chart, ChartFetchFailure>) result;
        assertInstanceOf(ChartFetchFailure.InvalidRequest.class, err.error());
        verifyNoInteractions(apiClient);
    }

    private static UpbitProps upbitProps(int maxCandleCountPerRequest) {
        UpbitProps props = org.mockito.Mockito.mock(UpbitProps.class);
        when(props.maxCandleCountPerRequest()).thenReturn(maxCandleCountPerRequest);
        return props;
    }

    private static UpbitCandleResponse[] buildCandles(String market, Instant earliest, int count, Duration step,
            long unit) {
        List<UpbitCandleResponse> res = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Instant ts = earliest.plus(step.multipliedBy(i));
            res.add(candle(market, ts, unit));
        }
        return res.toArray(UpbitCandleResponse[]::new);
    }

    private static UpbitCandleResponse candle(String market, Instant time, long unit) {
        return new UpbitCandleResponse(
                market,
                time.toString(),
                time.toString(),
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                time.toEpochMilli(),
                BigDecimal.ONE,
                BigDecimal.ONE,
                unit);
    }
}
