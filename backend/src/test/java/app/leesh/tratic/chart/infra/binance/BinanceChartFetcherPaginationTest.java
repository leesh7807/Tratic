package app.leesh.tratic.chart.infra.binance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.MarketSymbol;
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
    void fetch_callsApiOnceWhenRequestIsWithinPerCallLimit() {
        int maxPerCall = 256;
        Instant asOf = Instant.parse("2026-01-01T00:00:00Z");

        BinanceCandleResponse[] body = new BinanceCandleResponse[] {
                candle(asOf.minus(Duration.ofMinutes(10))),
                candle(asOf.minus(Duration.ofMinutes(5)))
        };
        when(apiClient.fetchCandlesTo(eq(signature()), eq("BTCUSDT"), eq("5m"), eq(asOf.toEpochMilli()), eq(256)))
                .thenReturn(Result.ok(body));

        BinanceChartFetcher fetcher = fetcher(maxPerCall);
        Result<Chart, ChartFetchFailure> result = fetcher.fetch(new ChartFetchRequest(signature(), asOf, 256));

        assertInstanceOf(Result.Ok.class, result);
        verify(apiClient).fetchCandlesTo(eq(signature()), eq("BTCUSDT"), eq("5m"), eq(asOf.toEpochMilli()), eq(256));
    }

    @Test
    void fetch_failsFastWhenRequestExceedsPerCallLimit() {
        BinanceChartFetcher fetcher = fetcher(256);
        ChartFetchRequest req = new ChartFetchRequest(signature(), Instant.parse("2026-01-01T00:00:00Z"), 257);

        Result<Chart, ChartFetchFailure> result = fetcher.fetch(req);

        assertInstanceOf(Result.Err.class, result);
        Result.Err<Chart, ChartFetchFailure> err = (Result.Err<Chart, ChartFetchFailure>) result;
        assertInstanceOf(ChartFetchFailure.InvalidRequest.class, err.error());
        verifyNoInteractions(apiClient);
    }

    @Test
    void fetch_deduplicatesByTimeWithinSingleBatch() {
        int maxPerCall = 256;
        Instant asOf = Instant.parse("2026-01-01T00:00:00Z");
        Instant duplicated = asOf.minus(Duration.ofMinutes(5));

        BinanceCandleResponse[] body = new BinanceCandleResponse[] {
                candle(asOf.minus(Duration.ofMinutes(10))),
                candle(duplicated),
                candle(duplicated)
        };
        when(apiClient.fetchCandlesTo(eq(signature()), eq("BTCUSDT"), eq("5m"), eq(asOf.toEpochMilli()), eq(3)))
                .thenReturn(Result.ok(body));

        BinanceChartFetcher fetcher = fetcher(maxPerCall);
        Result<Chart, ChartFetchFailure> result = fetcher.fetch(new ChartFetchRequest(signature(), asOf, 3));

        assertInstanceOf(Result.Ok.class, result);
        Chart chart = ((Result.Ok<Chart, ChartFetchFailure>) result).value();
        assertEquals(2, chart.candlesBeforeBucketOf(asOf).size());
    }

    private BinanceChartFetcher fetcher(int maxPerCall) {
        BinanceProps props = mock(BinanceProps.class);
        when(props.maxCandlesPerCall()).thenReturn(maxPerCall);
        return new BinanceChartFetcher(apiClient, new BinanceCandleResponseMapper(), props);
    }

    private static ChartSignature signature() {
        return new ChartSignature(Market.BINANCE, new MarketSymbol("BTCUSDT"), TimeResolution.M5);
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
