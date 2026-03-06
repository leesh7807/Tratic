package app.leesh.tratic.analyze.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.service.error.AnalyzeFailure;
import app.leesh.tratic.chart.domain.Candle;
import app.leesh.tratic.chart.domain.CandleSeries;
import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.Symbol;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.chart.service.ChartService;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@ExtendWith(MockitoExtension.class)
public class AnalyzeServiceTest {

    @Mock
    private ChartService chartService;

    @Mock
    private AnalyzePolicy analyzePolicy;

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @InjectMocks
    private AnalyzeService analyzeService;

    @Test
    public void analyze_saves_when_authenticated_user() {
        AnalyzeRequest req = new AnalyzeRequest(
                Market.BINANCE,
                "BTCUSDT",
                TimeResolution.M15,
                Instant.parse("2026-01-10T10:00:00Z"),
                bd("100"),
                bd("95"),
                bd("110"),
                bd("30"));

        when(analyzePolicy.fetchCandleCount()).thenReturn(240L);
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleChart(Market.BINANCE, "BTCUSDT")));

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, UUID.randomUUID());

        assertInstanceOf(Result.Ok.class, result);
        verify(analysisResultRepository).save(any(), any(), any());
    }

    @Test
    public void analyze_does_not_save_when_guest_user() {
        AnalyzeRequest req = new AnalyzeRequest(
                Market.UPBIT,
                "KRW-BTC",
                TimeResolution.M15,
                Instant.parse("2026-01-10T10:00:00Z"),
                bd("100"),
                bd("95"),
                bd("110"),
                null);

        when(analyzePolicy.fetchCandleCount()).thenReturn(240L);
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleChart(Market.UPBIT, "KRW-BTC")));

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, null);

        assertInstanceOf(Result.Ok.class, result);
        verify(analysisResultRepository, never()).save(any(), any(), any());
    }

    @Test
    public void analyze_returns_invalid_input_when_direction_cannot_be_inferred() {
        AnalyzeRequest req = new AnalyzeRequest(
                Market.UPBIT,
                "KRW-BTC",
                TimeResolution.M15,
                Instant.parse("2026-01-10T10:00:00Z"),
                bd("100"),
                bd("98"),
                bd("99"),
                null);

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, null);

        assertInstanceOf(Result.Err.class, result);
        AnalyzeFailure failure = ((Result.Err<AnalyzeResult, AnalyzeFailure>) result).error();
        assertInstanceOf(AnalyzeFailure.InvalidInput.class, failure);
        verify(chartService, never()).collectChart(any());
    }

    @Test
    public void analyze_maps_chart_failure() {
        AnalyzeRequest req = new AnalyzeRequest(
                Market.BINANCE,
                "BTCUSDT",
                TimeResolution.M15,
                Instant.parse("2026-01-10T10:00:00Z"),
                bd("100"),
                bd("95"),
                bd("110"),
                bd("50"));

        when(analyzePolicy.fetchCandleCount()).thenReturn(240L);
        when(chartService.collectChart(any())).thenReturn(Result.err(new ChartFetchFailure.RateLimited(Market.BINANCE, null)));

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, UUID.randomUUID());

        assertInstanceOf(Result.Err.class, result);
        AnalyzeFailure failure = ((Result.Err<AnalyzeResult, AnalyzeFailure>) result).error();
        assertInstanceOf(AnalyzeFailure.ChartDataUnavailable.class, failure);
    }

    private Chart sampleChart(Market market, String symbol) {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 260; i++) {
            BigDecimal open = bd(100 + i * 0.1);
            BigDecimal close = bd(100 + i * 0.1 + (i % 3) * 0.02);
            BigDecimal high = close.max(open).add(bd("0.3"));
            BigDecimal low = close.min(open).subtract(bd("0.3"));
            candles.add(new Candle(
                    start.plusSeconds(i * 15L * 60L),
                    open,
                    high,
                    low,
                    close,
                    bd(1000 + i * 10)));
        }

        ChartSignature signature = new ChartSignature(market, new Symbol(symbol), TimeResolution.M15);
        return Chart.of(signature, CandleSeries.ofSorted(candles));
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }
}
