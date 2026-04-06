package app.leesh.tratic.analyze.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.AnalyzeEngineParams;
import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.domain.AnalyzeSpec;
import app.leesh.tratic.analyze.domain.AnalyzeSpecResolver;
import app.leesh.tratic.analyze.domain.band.AnalyzeBandRange;
import app.leesh.tratic.analyze.domain.band.AnalyzeBandSet;
import app.leesh.tratic.analyze.domain.band.AnalyzeBandSpec;
import app.leesh.tratic.analyze.domain.band.LocationBand;
import app.leesh.tratic.analyze.domain.band.PressureBand;
import app.leesh.tratic.analyze.domain.band.TrendBand;
import app.leesh.tratic.analyze.service.AnalyzeRequest;
import app.leesh.tratic.analyze.service.AnalyzeService;
import app.leesh.tratic.analyze.service.error.AnalyzeFailure;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.shared.Result;

@ExtendWith(MockitoExtension.class)
public class AnalyzeControllerTest {

    @Mock
    private AnalyzeService analyzeService;

    @Mock
    private AnalyzeSpecResolver analyzeSpecResolver;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private AnalyzeController analyzeController;

    @Test
    @DisplayName("비로그인 사용자는 null 사용자 ID로 전달한다")
    public void analyze_guest_user_passes_null_user_id() {
        AnalyzeRequestDto request = requestDto();

        when(analyzeSpecResolver.resolve(TimeResolution.M15)).thenReturn(spec());
        when(analyzeService.analyze(any(), eq(null))).thenReturn(Result.ok(sampleResult()));

        ResponseEntity<?> response = analyzeController.analyze(request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AnalyzeResponseDto body = assertResponseBody(response);
        assertEquals("상승", body.trend());
        assertEquals("상단", body.location());
        assertEquals("매수 우세", body.pressure());
        verify(analyzeService).analyze(any(AnalyzeRequest.class), eq(null));
    }

    @Test
    @DisplayName("로그인 사용자는 principal의 사용자 ID를 사용한다")
    public void analyze_authenticated_user_uses_user_id_from_principal() {
        AnalyzeRequestDto request = requestDto();
        UUID userId = UUID.randomUUID();

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("userId", userId.toString()));
        when(analyzeSpecResolver.resolve(TimeResolution.M15)).thenReturn(spec());
        when(analyzeService.analyze(any(), eq(userId))).thenReturn(Result.ok(sampleResult()));

        ResponseEntity<?> response = analyzeController.analyze(request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AnalyzeResponseDto body = assertResponseBody(response);
        assertEquals("매수 우세", body.pressure());
        ArgumentCaptor<AnalyzeRequest> captor = ArgumentCaptor.forClass(AnalyzeRequest.class);
        verify(analyzeService).analyze(captor.capture(), eq(userId));
        assertEquals(TimeResolution.M15, captor.getValue().resolution());
        assertEquals(AnalyzeDirection.LONG, captor.getValue().direction());
    }

    @Test
    @DisplayName("InvalidInput은 잘못된 요청으로 매핑한다")
    public void analyze_invalid_input_maps_bad_request() {
        AnalyzeRequestDto request = requestDto();
        when(analyzeService.analyze(any(), eq(null)))
                .thenReturn(Result.err(new AnalyzeFailure.InvalidInput("bad input")));

        ResponseEntity<?> response = analyzeController.analyze(request, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
    }

    @Test
    @DisplayName("캔들 부족은 처리할 수 없는 엔티티로 매핑한다")
    public void analyze_insufficient_candles_maps_unprocessable_entity() {
        AnalyzeRequestDto request = requestDto();
        when(analyzeService.analyze(any(), eq(null)))
                .thenReturn(Result.err(new AnalyzeFailure.InsufficientCandles(105, 80)));

        ResponseEntity<?> response = analyzeController.analyze(request, null);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
    }

    private AnalyzeRequestDto requestDto() {
        return new AnalyzeRequestDto(
                Market.BINANCE,
                "BTCUSDT",
                TimeResolution.M15,
                Instant.parse("2026-01-10T10:00:00Z"),
                new BigDecimal("100"),
                AnalyzeDirection.LONG);
    }

    private AnalyzeResult sampleResult() {
        return new AnalyzeResult(
                AnalyzeDirection.LONG,
                42.0,
                12.0,
                68.0,
                35.0,
                0.35,
                0.21);
    }

    private AnalyzeSpec spec() {
        return new AnalyzeSpec(
                new AnalyzeEngineParams(
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
                        1.5),
                new AnalyzeBandSpec(
                        new AnalyzeBandSet<>(java.util.List.of(
                                new AnalyzeBandRange<>(TrendBand.STRONG_BEAR, -100.0, -60.0),
                                new AnalyzeBandRange<>(TrendBand.BEAR, -60.0, -20.0),
                                new AnalyzeBandRange<>(TrendBand.NEUTRAL, -20.0, 20.0),
                                new AnalyzeBandRange<>(TrendBand.BULL, 20.0, 60.0),
                                new AnalyzeBandRange<>(TrendBand.STRONG_BULL, 60.0, 100.000001))),
                        new AnalyzeBandSet<>(java.util.List.of(
                                new AnalyzeBandRange<>(LocationBand.LOWEST, 0.0, 20.0),
                                new AnalyzeBandRange<>(LocationBand.LOWER, 20.0, 40.0),
                                new AnalyzeBandRange<>(LocationBand.MIDDLE, 40.0, 60.0),
                                new AnalyzeBandRange<>(LocationBand.UPPER, 60.0, 80.0),
                                new AnalyzeBandRange<>(LocationBand.HIGHEST, 80.0, 100.000001))),
                        new AnalyzeBandSet<>(java.util.List.of(
                                new AnalyzeBandRange<>(PressureBand.STRONG_SELL, -100.0, -60.0),
                                new AnalyzeBandRange<>(PressureBand.SELL, -60.0, -20.0),
                                new AnalyzeBandRange<>(PressureBand.NEUTRAL, -20.0, 20.0),
                                new AnalyzeBandRange<>(PressureBand.BUY, 20.0, 60.0),
                                new AnalyzeBandRange<>(PressureBand.STRONG_BUY, 60.0, 100.000001)))));
    }

    private AnalyzeResponseDto assertResponseBody(ResponseEntity<?> response) {
        Object body = response.getBody();
        assertNotNull(body);
        return assertInstanceOf(AnalyzeResponseDto.class, body);
    }
}
