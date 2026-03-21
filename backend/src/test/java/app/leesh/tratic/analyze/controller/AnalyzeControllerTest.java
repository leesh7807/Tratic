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
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeScenario;
import app.leesh.tratic.analyze.service.AnalyzeInterpretationRenderer;
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
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private AnalyzeInterpretationRenderer interpretationRenderer;

    @InjectMocks
    private AnalyzeController analyzeController;

    @Test
    @DisplayName("비로그인 사용자는 null 사용자 ID로 전달한다")
    public void analyze_guest_user_passes_null_user_id() {
        AnalyzeRequestDto request = requestDto();

        when(analyzeService.analyze(any(), eq(null))).thenReturn(Result.ok(sampleResult()));
        when(interpretationRenderer.render(any())).thenReturn("요약");

        ResponseEntity<?> response = analyzeController.analyze(request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AnalyzeResponseDto body = assertResponseBody(response);
        assertEquals(AnalyzeScenario.BULLISH_TREND_CONTINUATION, body.scenario());
        assertEquals("요약", body.summary());
        verify(analyzeService).analyze(any(AnalyzeRequest.class), eq(null));
    }

    @Test
    @DisplayName("로그인 사용자는 principal의 사용자 ID를 사용한다")
    public void analyze_authenticated_user_uses_user_id_from_principal() {
        AnalyzeRequestDto request = requestDto();
        UUID userId = UUID.randomUUID();

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("userId", userId.toString()));
        when(analyzeService.analyze(any(), eq(userId))).thenReturn(Result.ok(sampleResult()));
        when(interpretationRenderer.render(any())).thenReturn("요약");

        ResponseEntity<?> response = analyzeController.analyze(request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AnalyzeResponseDto body = assertResponseBody(response);
        assertEquals("요약", body.summary());
        ArgumentCaptor<AnalyzeRequest> captor = ArgumentCaptor.forClass(AnalyzeRequest.class);
        verify(analyzeService).analyze(captor.capture(), eq(userId));
        assertEquals(TimeResolution.M15, captor.getValue().resolution());
        assertEquals(new BigDecimal("25.5"), captor.getValue().positionPct());
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
                new BigDecimal("90"),
                new BigDecimal("25.5"));
    }

    private AnalyzeInterpretation sampleResult() {
        return new AnalyzeInterpretation(
                AnalyzeDirection.LONG,
                AnalyzeScenario.BULLISH_TREND_CONTINUATION,
                "CONTINUATION",
                "HIGH",
                "MEDIUM",
                "matrix-v1");
    }

    private AnalyzeResponseDto assertResponseBody(ResponseEntity<?> response) {
        Object body = response.getBody();
        assertNotNull(body);
        return assertInstanceOf(AnalyzeResponseDto.class, body);
    }
}
