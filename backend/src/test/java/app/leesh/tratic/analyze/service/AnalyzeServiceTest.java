package app.leesh.tratic.analyze.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import app.leesh.tratic.analyze.domain.AnalyzeEngine;
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
    private static final TimeResolution RESOLUTION = TimeResolution.M15;
    private static final Instant ENTRY_AT = Instant.parse("2026-01-10T10:00:00Z");


    @Mock
    private ChartService chartService;

    @Mock
    private AnalyzeFetchCountConfig analyzeFetchCountConfig;

    @Mock
    private AnalyzeEngine analyzeEngine;

    @Mock
    private AnalyzeResultRepository analyzeResultRepository;

    @InjectMocks
    private AnalyzeService analyzeService;

    @Test
    @DisplayName("로그인 사용자면 분석 결과를 저장한다")
    public void analyze_saves_when_authenticated_user() {
        AnalyzeRequest req = request(Market.BINANCE, "BTCUSDT", ENTRY_AT, "100", AnalyzeDirection.LONG);

        when(analyzeFetchCountConfig.fetchCandleCount()).thenReturn(240L);
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleChart(Market.BINANCE, "BTCUSDT")));
        when(analyzeEngine.minimumRequiredCandles(RESOLUTION)).thenReturn(105);
        when(analyzeEngine.analyze(any(), eq(RESOLUTION), eq(AnalyzeDirection.LONG)))
                .thenReturn(sampleAnalyzeResult(AnalyzeDirection.LONG));

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, UUID.randomUUID());

        assertInstanceOf(Result.Ok.class, result);
        verify(analyzeResultRepository).save(any(), any(), any());
    }

    @Test
    @DisplayName("비로그인 사용자면 분석 결과를 저장하지 않는다")
    public void analyze_does_not_save_when_guest_user() {
        AnalyzeRequest req = request(Market.UPBIT, "KRW-BTC", ENTRY_AT, "100", AnalyzeDirection.LONG);

        when(analyzeFetchCountConfig.fetchCandleCount()).thenReturn(240L);
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleChart(Market.UPBIT, "KRW-BTC")));
        when(analyzeEngine.minimumRequiredCandles(RESOLUTION)).thenReturn(105);
        when(analyzeEngine.analyze(any(), eq(RESOLUTION), eq(AnalyzeDirection.LONG)))
                .thenReturn(sampleAnalyzeResult(AnalyzeDirection.LONG));

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, null);

        assertInstanceOf(Result.Ok.class, result);
        verify(analyzeResultRepository, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("direction이 LONG이면 LONG으로 분석한다")
    public void analyze_uses_long_direction_from_request() {
        AnalyzeRequest req = request(Market.UPBIT, "KRW-BTC", ENTRY_AT, "100", AnalyzeDirection.LONG);

        when(analyzeFetchCountConfig.fetchCandleCount()).thenReturn(240L);
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleChart(Market.UPBIT, "KRW-BTC")));
        when(analyzeEngine.minimumRequiredCandles(RESOLUTION)).thenReturn(105);
        when(analyzeEngine.analyze(any(), eq(RESOLUTION), eq(AnalyzeDirection.LONG)))
                .thenReturn(sampleAnalyzeResult(AnalyzeDirection.LONG));

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, null);

        assertInstanceOf(Result.Ok.class, result);
    }

    @Test
    @DisplayName("direction이 SHORT면 SHORT로 분석한다")
    public void analyze_uses_short_direction_from_request() {
        AnalyzeRequest req = request(Market.BINANCE, "BTCUSDT", ENTRY_AT, "100", AnalyzeDirection.SHORT);

        when(analyzeFetchCountConfig.fetchCandleCount()).thenReturn(240L);
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleChart(Market.BINANCE, "BTCUSDT")));
        when(analyzeEngine.minimumRequiredCandles(RESOLUTION)).thenReturn(105);
        when(analyzeEngine.analyze(any(), eq(RESOLUTION), eq(AnalyzeDirection.SHORT)))
                .thenReturn(sampleAnalyzeResult(AnalyzeDirection.SHORT));

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, null);

        assertInstanceOf(Result.Ok.class, result);
    }

    @Test
    @DisplayName("차트 수집 실패는 분석 실패로 매핑한다")
    public void analyze_maps_chart_failure() {
        AnalyzeRequest req = request(Market.BINANCE, "BTCUSDT", ENTRY_AT, "100", AnalyzeDirection.LONG);

        when(analyzeFetchCountConfig.fetchCandleCount()).thenReturn(240L);
        when(chartService.collectChart(any())).thenReturn(Result.err(new ChartFetchFailure.RateLimited(Market.BINANCE, null)));

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, UUID.randomUUID());

        assertInstanceOf(Result.Err.class, result);
        AnalyzeFailure failure = ((Result.Err<AnalyzeResult, AnalyzeFailure>) result).error();
        assertInstanceOf(AnalyzeFailure.ChartDataUnavailable.class, failure);
    }

    @Test
    @DisplayName("수집된 캔들이 부족하면 InsufficientCandles를 반환한다")
    public void analyze_returns_insufficient_candles_when_collected_data_is_too_short() {
        AnalyzeRequest req = request(Market.BINANCE, "BTCUSDT", ENTRY_AT, "100", AnalyzeDirection.LONG);

        when(analyzeFetchCountConfig.fetchCandleCount()).thenReturn(120L);
        when(chartService.collectChart(any())).thenReturn(Result.ok(sampleShortChart(Market.BINANCE, "BTCUSDT")));
        when(analyzeEngine.minimumRequiredCandles(RESOLUTION)).thenReturn(105);

        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(req, UUID.randomUUID());

        assertInstanceOf(Result.Err.class, result);
        AnalyzeFailure failure = ((Result.Err<AnalyzeResult, AnalyzeFailure>) result).error();
        assertInstanceOf(AnalyzeFailure.InsufficientCandles.class, failure);
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

    private AnalyzeRequest request(Market market, String symbol, Instant entryAt, String entryPrice,
            AnalyzeDirection direction) {
        return new AnalyzeRequest(
                market,
                symbol,
                RESOLUTION,
                entryAt,
                bd(entryPrice),
                direction);
    }

    private AnalyzeResult sampleAnalyzeResult(AnalyzeDirection direction) {
        return new AnalyzeResult(
                direction,
                42.0,
                12.0,
                68.0,
                35.0,
                0.35,
                0.21);
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }
}
