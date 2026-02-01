package app.leesh.tratic.chart.infra.upbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
import app.leesh.tratic.chart.service.ChartFetchRequest;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@ExtendWith(MockitoExtension.class)
public class UpbitChartFetcherSplitTest {

    @Mock
    private UpbitApiClient apiClient;

    @Test
    public void fetch_splitsRequestsInto200Chunks_and_rollsBackToEarliestMinusResolution() {
        Duration step = Duration.ofMinutes(5);
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

        UpbitChartFetcher fetcher = new UpbitChartFetcher(apiClient, new UpbitCandleResponseMapper());
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
    public void fetch_continuesWhenResponseIsPartial() {
        Duration step = Duration.ofMinutes(5);
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

        UpbitChartFetcher fetcher = new UpbitChartFetcher(apiClient, new UpbitCandleResponseMapper());
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
    public void fetch_stopsWhenResponseIsEmpty() {
        Duration step = Duration.ofMinutes(5);
        int firstCount = 200;

        Instant firstEarliest = Instant.parse("2026-01-01T00:00:00Z");
        Instant firstLatest = firstEarliest.plus(step.multipliedBy(firstCount - 1));

        UpbitCandleResponse[] batch1 = buildCandles("KRW-BTC", firstEarliest, firstCount, step, 5);
        UpbitCandleResponse[] empty = new UpbitCandleResponse[0];

        when(apiClient.fetchMinuteCandles(anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn(Result.ok(batch1))
                .thenReturn(Result.ok(empty));

        UpbitChartFetcher fetcher = new UpbitChartFetcher(apiClient, new UpbitCandleResponseMapper());
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

    private static UpbitCandleResponse[] buildCandles(String market, Instant earliest, int count, Duration step,
            long unit) {
        List<UpbitCandleResponse> res = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Instant ts = earliest.plus(step.multipliedBy(i));
            res.add(new UpbitCandleResponse(
                    market,
                    ts.toString(),
                    ts.toString(),
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    ts.toEpochMilli(),
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    unit));
        }
        return res.toArray(UpbitCandleResponse[]::new);
    }
}
