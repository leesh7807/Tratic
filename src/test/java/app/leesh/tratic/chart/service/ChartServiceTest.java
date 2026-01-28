package app.leesh.tratic.chart.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.Symbol;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@ExtendWith(MockitoExtension.class)
public class ChartServiceTest {

    @Mock
    private ChartFetcherResolver resolver;

    @Mock
    private ChartFetcher fetcher;

    @InjectMocks
    private ChartService service;

    @Test
    public void collectChart_returnsOkResultWithChart() {
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, Instant.parse("2026-01-01T00:00:00Z"), 1);
        Chart expectedChart = mock(Chart.class);

        when(resolver.resolve(Market.UPBIT)).thenReturn(fetcher);
        when(fetcher.fetch(req)).thenReturn(Result.ok(expectedChart));

        Result<Chart, ChartFetchFailure> result = service.collectChart(req);

        assertInstanceOf(Result.Ok.class, result);
        var okResult = (Result.Ok<Chart, ChartFetchFailure>) result;
        assertSame(expectedChart, okResult.value());
    }

    @Test
    public void collectChart_returnsErrResultWithTemporaryFailure() {
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, Instant.parse("2026-01-01T00:00:00Z"), 1);

        when(resolver.resolve(Market.UPBIT)).thenReturn(fetcher);
        when(fetcher.fetch(req)).thenReturn(Result.err(new ChartFetchFailure.Temporary(Market.UPBIT)));

        Result<Chart, ChartFetchFailure> result = service.collectChart(req);

        assertInstanceOf(Result.Err.class, result);
        var err = (Result.Err<Chart, ChartFetchFailure>) result;
        assertInstanceOf(ChartFetchFailure.Temporary.class, err.error());
    }

    @Test
    public void collectChart_returnsErrResultWithRateLimitedFailure() {
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, Instant.parse("2026-01-01T00:00:00Z"), 1);

        when(resolver.resolve(Market.UPBIT)).thenReturn(fetcher);
        when(fetcher.fetch(req))
                .thenReturn(Result.err(new ChartFetchFailure.RateLimited(Market.UPBIT, java.time.Duration.ofSeconds(5))));

        Result<Chart, ChartFetchFailure> result = service.collectChart(req);

        assertInstanceOf(Result.Err.class, result);
        var err = (Result.Err<Chart, ChartFetchFailure>) result;
        assertInstanceOf(ChartFetchFailure.RateLimited.class, err.error());
    }

    @Test
    public void collectChart_returnsErrResultWithInvalidRequestFailure() {
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, Instant.parse("2026-01-01T00:00:00Z"), 1);

        when(resolver.resolve(Market.UPBIT)).thenReturn(fetcher);
        when(fetcher.fetch(req)).thenReturn(Result.err(new ChartFetchFailure.InvalidRequest(Market.UPBIT)));

        Result<Chart, ChartFetchFailure> result = service.collectChart(req);

        assertInstanceOf(Result.Err.class, result);
        var err = (Result.Err<Chart, ChartFetchFailure>) result;
        assertInstanceOf(ChartFetchFailure.InvalidRequest.class, err.error());
    }

    @Test
    public void collectChart_returnsErrResultWithUnauthorizedFailure() {
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, Instant.parse("2026-01-01T00:00:00Z"), 1);

        when(resolver.resolve(Market.UPBIT)).thenReturn(fetcher);
        when(fetcher.fetch(req)).thenReturn(Result.err(new ChartFetchFailure.Unauthorized(Market.UPBIT)));

        Result<Chart, ChartFetchFailure> result = service.collectChart(req);

        assertInstanceOf(Result.Err.class, result);
        var err = (Result.Err<Chart, ChartFetchFailure>) result;
        assertInstanceOf(ChartFetchFailure.Unauthorized.class, err.error());
    }

    @Test
    public void collectChart_returnsErrResultWithNotFoundFailure() {
        ChartSignature sig = new ChartSignature(Market.UPBIT, new Symbol("KRW-BTC"), TimeResolution.M5);
        ChartFetchRequest req = new ChartFetchRequest(sig, Instant.parse("2026-01-01T00:00:00Z"), 1);

        when(resolver.resolve(Market.UPBIT)).thenReturn(fetcher);
        when(fetcher.fetch(req)).thenReturn(Result.err(new ChartFetchFailure.NotFound(Market.UPBIT)));

        Result<Chart, ChartFetchFailure> result = service.collectChart(req);

        assertInstanceOf(Result.Err.class, result);
        var err = (Result.Err<Chart, ChartFetchFailure>) result;
        assertInstanceOf(ChartFetchFailure.NotFound.class, err.error());
    }

}
