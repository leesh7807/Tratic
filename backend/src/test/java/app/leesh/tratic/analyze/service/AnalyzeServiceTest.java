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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.AnalysisEngineParams;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeScenario;
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
    private static final TimeResolution RESOLUTION = TimeResolution.M15;
    private static final Instant ENTRY_AT = Instant.parse("2026-01-10T10:00:00Z");


    @Mock
    private ChartService chartService;

    @Mock
    private AnalyzePolicy analyzePolicy;

    @Mock
    private AnalysisEnginePolicy analysisEnginePolicy;

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @Mock
    private AnalyzeInterpreter analyzeInterpreter;

    @InjectMocks
    private AnalyzeService analyzeService;

    @Test
    @DisplayName("로그인 사용자면 분석 결과를 저장한다")
    public void analyze_saves_when_authenticated_user() {
        AnalyzeRequest req = request(Market.BINANCE, "BTCUSDT", ENTRY_AT, "100", "95", "110", "30");

        when(analyzePolicy.fetchCandleCount()).thenReturn(240L);
        when(analysisEnginePolicy.resolve(RESOLUTION)).thenReturn(defaultEngineParams());
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleChart(Market.BINANCE, "BTCUSDT")));
        when(analyzeInterpreter.interpret(any())).thenReturn(sampleInterpretation());

        Result<AnalyzeInterpretation, AnalyzeFailure> result = analyzeService.analyze(req, UUID.randomUUID());

        assertInstanceOf(Result.Ok.class, result);
        verify(analysisResultRepository).save(any(), any(), any(), any());
    }

    @Test
    @DisplayName("비로그인 사용자면 분석 결과를 저장하지 않는다")
    public void analyze_does_not_save_when_guest_user() {
        AnalyzeRequest req = request(Market.UPBIT, "KRW-BTC", ENTRY_AT, "100", "95", "110", null);

        when(analyzePolicy.fetchCandleCount()).thenReturn(240L);
        when(analysisEnginePolicy.resolve(RESOLUTION)).thenReturn(defaultEngineParams());
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleChart(Market.UPBIT, "KRW-BTC")));
        when(analyzeInterpreter.interpret(any())).thenReturn(sampleInterpretation());

        Result<AnalyzeInterpretation, AnalyzeFailure> result = analyzeService.analyze(req, null);

        assertInstanceOf(Result.Ok.class, result);
        verify(analysisResultRepository, never()).save(any(), any(), any(), any());
    }

    @Test
    @DisplayName("방향을 추론할 수 없으면 InvalidInput을 반환한다")
    public void analyze_returns_invalid_input_when_direction_cannot_be_inferred() {
        AnalyzeRequest req = request(Market.UPBIT, "KRW-BTC", ENTRY_AT, "100", "98", "99", null);

        Result<AnalyzeInterpretation, AnalyzeFailure> result = analyzeService.analyze(req, null);

        assertInstanceOf(Result.Err.class, result);
        AnalyzeFailure failure = ((Result.Err<AnalyzeInterpretation, AnalyzeFailure>) result).error();
        assertInstanceOf(AnalyzeFailure.InvalidInput.class, failure);
        verify(chartService, never()).collectChart(any());
    }

    @Test
    @DisplayName("손절가나 익절가가 진입가와 같으면 InvalidInput을 반환한다")
    public void analyze_returns_invalid_input_when_price_equals_entry() {
        AnalyzeRequest req = request(Market.UPBIT, "KRW-BTC", ENTRY_AT, "100", "100", "110", null);

        Result<AnalyzeInterpretation, AnalyzeFailure> result = analyzeService.analyze(req, null);

        assertInstanceOf(Result.Err.class, result);
        AnalyzeFailure failure = ((Result.Err<AnalyzeInterpretation, AnalyzeFailure>) result).error();
        assertInstanceOf(AnalyzeFailure.InvalidInput.class, failure);
        verify(chartService, never()).collectChart(any());
    }

    @Test
    @DisplayName("차트 수집 실패는 분석 실패로 매핑한다")
    public void analyze_maps_chart_failure() {
        AnalyzeRequest req = request(Market.BINANCE, "BTCUSDT", ENTRY_AT, "100", "95", "110", "50");

        when(analyzePolicy.fetchCandleCount()).thenReturn(240L);
        when(chartService.collectChart(any())).thenReturn(Result.err(new ChartFetchFailure.RateLimited(Market.BINANCE, null)));

        Result<AnalyzeInterpretation, AnalyzeFailure> result = analyzeService.analyze(req, UUID.randomUUID());

        assertInstanceOf(Result.Err.class, result);
        AnalyzeFailure failure = ((Result.Err<AnalyzeInterpretation, AnalyzeFailure>) result).error();
        assertInstanceOf(AnalyzeFailure.ChartDataUnavailable.class, failure);
    }

    @Test
    @DisplayName("수집된 캔들이 부족하면 InsufficientCandles를 반환한다")
    public void analyze_returns_insufficient_candles_when_collected_data_is_too_short() {
        AnalyzeRequest req = request(Market.BINANCE, "BTCUSDT", ENTRY_AT, "100", "95", "110", "50");

        when(analyzePolicy.fetchCandleCount()).thenReturn(120L);
        when(analysisEnginePolicy.resolve(RESOLUTION)).thenReturn(defaultEngineParams());
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleShortChart(Market.BINANCE, "BTCUSDT")));

        Result<AnalyzeInterpretation, AnalyzeFailure> result = analyzeService.analyze(req, UUID.randomUUID());

        assertInstanceOf(Result.Err.class, result);
        AnalyzeFailure failure = ((Result.Err<AnalyzeInterpretation, AnalyzeFailure>) result).error();
        assertInstanceOf(AnalyzeFailure.InsufficientCandles.class, failure);
    }

    private AnalyzeInterpretation sampleInterpretation() {
        return new AnalyzeInterpretation(
                AnalyzeDirection.LONG,
                AnalyzeScenario.BULLISH_TREND_CONTINUATION,
                "CONTINUATION",
                "HIGH",
                "MEDIUM",
                "matrix-v1");
    }

    private Chart sampleChart(Market market, String symbol) {
        return sampleChart(market, symbol, 260);
    }

    private Chart sampleShortChart(Market market, String symbol) {
        return sampleChart(market, symbol, 40);
    }

    private Chart sampleChart(Market market, String symbol, int candleCount) {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < candleCount; i++) {
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

    private AnalyzeRequest request(Market market, String symbol, Instant entryAt, String entryPrice, String stopLossPrice,
            String takeProfitPrice, String positionPct) {
        return new AnalyzeRequest(
                market,
                symbol,
                RESOLUTION,
                entryAt,
                bd(entryPrice),
                bd(stopLossPrice),
                bd(takeProfitPrice),
                positionPct == null ? null : bd(positionPct));
    }

    // Mirrors the current analyze-engine.yml defaults as hard-coded test data.
    private AnalysisEngineParams defaultEngineParams() {
        return new AnalysisEngineParams(
                1e-9,
                20,
                10,
                30,
                0.1,
                1e-6,
                20,
                30,
                3.0,
                1.45,
                0.65,
                20,
                14,
                100,
                5,
                20,
                5,
                0.6,
                0.3,
                0.1,
                0.5,
                1.5);
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }
}
