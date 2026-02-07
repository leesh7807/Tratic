package app.leesh.tratic.chart.infra.binance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyInt;
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
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.BinanceProps;
import app.leesh.tratic.chart.service.ChartFetchRequest;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@ExtendWith(MockitoExtension.class)
class BinanceChartFetcherPaginationTest {

    @Mock
    private BinanceApiClient apiClient;

    @Test
    void fetch_splitsByMaxCandlesAnd_stepsEndTimeByResolution() {
        Duration step = Duration.ofMinutes(5);
        int maxPerCall = 100;
        int totalCount = 250;

        Instant firstEarliest = Instant.parse("2026-01-01T00:00:00Z");
        Instant firstLatest = firstEarliest.plus(step.multipliedBy(maxPerCall - 1));
        Instant secondEarliest = firstEarliest.minus(step.multipliedBy(maxPerCall));
        Instant thirdEarliest = secondEarliest.minus(step.multipliedBy(50));

        BinanceCandleResponse[] batch1 = buildCandles(firstEarliest, maxPerCall, step);
        BinanceCandleResponse[] batch2 = buildCandles(secondEarliest, maxPerCall, step);
        BinanceCandleResponse[] batch3 = buildCandles(thirdEarliest, 50, step);

        when(apiClient.fetchCandlesTo(eq(signature()), anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(Result.ok(batch1))
                .thenReturn(Result.ok(batch2))
                .thenReturn(Result.ok(batch3));

        BinanceChartFetcher fetcher = fetcher(maxPerCall);
        Result<Chart, ChartFetchFailure> result = fetcher
                .fetch(new ChartFetchRequest(signature(), firstLatest, totalCount));

        assertInstanceOf(Result.Ok.class, result);

        ArgumentCaptor<Long> toCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(apiClient, times(3)).fetchCandlesTo(eq(signature()), eq("BTCUSDT"), eq("5m"), toCaptor.capture(),
                limitCaptor.capture());

        List<Long> toValues = toCaptor.getAllValues();
        List<Integer> limitValues = limitCaptor.getAllValues();

        assertEquals(firstLatest.toEpochMilli(), toValues.get(0));
        assertEquals(maxPerCall, limitValues.get(0));

        assertEquals(firstEarliest.minus(step).toEpochMilli(), toValues.get(1));
        assertEquals(maxPerCall, limitValues.get(1));

        assertEquals(secondEarliest.minus(step).toEpochMilli(), toValues.get(2));
        assertEquals(50, limitValues.get(2));
    }

    @Test
    void fetch_stopsWhenResponseIsEmpty() {
        Duration step = Duration.ofMinutes(5);
        int maxPerCall = 100;
        Instant firstEarliest = Instant.parse("2026-01-01T00:00:00Z");
        Instant firstLatest = firstEarliest.plus(step.multipliedBy(maxPerCall - 1));

        BinanceCandleResponse[] batch1 = buildCandles(firstEarliest, maxPerCall, step);
        BinanceCandleResponse[] empty = new BinanceCandleResponse[0];

        when(apiClient.fetchCandlesTo(eq(signature()), anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(Result.ok(batch1))
                .thenReturn(Result.ok(empty));

        BinanceChartFetcher fetcher = fetcher(maxPerCall);
        Result<Chart, ChartFetchFailure> result = fetcher
                .fetch(new ChartFetchRequest(signature(), firstLatest, 300));

        assertInstanceOf(Result.Ok.class, result);
        verify(apiClient, times(2)).fetchCandlesTo(eq(signature()), eq("BTCUSDT"), eq("5m"), anyLong(), anyInt());
    }

    @Test
    void fetch_stopsWhenRequestedCountReached() {
        Duration step = Duration.ofMinutes(5);
        int maxPerCall = 100;
        Instant firstEarliest = Instant.parse("2026-01-01T00:00:00Z");
        Instant firstLatest = firstEarliest.plus(step.multipliedBy(maxPerCall - 1));
        Instant secondEarliest = firstEarliest.minus(step.multipliedBy(80));

        BinanceCandleResponse[] batch1 = buildCandles(firstEarliest, maxPerCall, step);
        BinanceCandleResponse[] batch2 = buildCandles(secondEarliest, 80, step);

        when(apiClient.fetchCandlesTo(eq(signature()), anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(Result.ok(batch1))
                .thenReturn(Result.ok(batch2));

        BinanceChartFetcher fetcher = fetcher(maxPerCall);
        Result<Chart, ChartFetchFailure> result = fetcher
                .fetch(new ChartFetchRequest(signature(), firstLatest, 180));

        assertInstanceOf(Result.Ok.class, result);
        verify(apiClient, times(2)).fetchCandlesTo(eq(signature()), eq("BTCUSDT"), eq("5m"), anyLong(), anyInt());
    }

    @Test
    void fetch_deduplicatesAcrossBatches_beforeDomainValidation() {
        Duration step = Duration.ofMinutes(5);
        int maxPerCall = 100;
        Instant firstEarliest = Instant.parse("2026-01-01T00:00:00Z");
        Instant firstLatest = firstEarliest.plus(step.multipliedBy(maxPerCall - 1));

        BinanceCandleResponse[] batch1 = buildCandles(firstEarliest, maxPerCall, step);
        BinanceCandleResponse[] batch2 = buildCandlesWithLeadingDuplicate(firstEarliest, 50, step);

        when(apiClient.fetchCandlesTo(eq(signature()), anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(Result.ok(batch1))
                .thenReturn(Result.ok(batch2));

        BinanceChartFetcher fetcher = fetcher(maxPerCall);
        Result<Chart, ChartFetchFailure> result = fetcher
                .fetch(new ChartFetchRequest(signature(), firstLatest, 150));

        assertInstanceOf(Result.Ok.class, result);
        verify(apiClient, times(2)).fetchCandlesTo(eq(signature()), eq("BTCUSDT"), eq("5m"), anyLong(), anyInt());
    }

    private BinanceChartFetcher fetcher(int maxPerCall) {
        return new BinanceChartFetcher(
                apiClient,
                new BinanceCandleResponseMapper(),
                new BinanceProps("https://fapi.binance.com", maxPerCall));
    }

    private static ChartSignature signature() {
        return new ChartSignature(Market.BINANCE, new Symbol("BTCUSDT"), TimeResolution.M5);
    }

    private static BinanceCandleResponse[] buildCandles(Instant earliest, int count, Duration step) {
        List<BinanceCandleResponse> res = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Instant ts = earliest.plus(step.multipliedBy(i));
            res.add(candle(ts));
        }
        return res.toArray(BinanceCandleResponse[]::new);
    }

    private static BinanceCandleResponse[] buildCandlesWithLeadingDuplicate(Instant duplicated, int count,
            Duration step) {
        List<BinanceCandleResponse> res = new ArrayList<>(count);
        res.add(candle(duplicated));
        Instant earliest = duplicated.minus(step.multipliedBy(count - 1));
        for (int i = 0; i < count - 1; i++) {
            Instant ts = earliest.plus(step.multipliedBy(i));
            res.add(candle(ts));
        }
        return res.toArray(BinanceCandleResponse[]::new);
    }

    private static BinanceCandleResponse candle(Instant ts) {
        return new BinanceCandleResponse(
                ts.toEpochMilli(),
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                ts.plusSeconds(60).toEpochMilli(),
                BigDecimal.ONE,
                1L,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO);
    }
}
