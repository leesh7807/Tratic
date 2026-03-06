package app.leesh.tratic.analyze.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.domain.VolatilityLabel;
import app.leesh.tratic.analyze.service.AnalyzeRequest;
import app.leesh.tratic.analyze.service.AnalyzeService;
import app.leesh.tratic.analyze.service.error.AnalyzeFailure;
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

    @InjectMocks
    private AnalyzeController analyzeController;

    @Test
    public void analyze_guest_user_passes_null_user_id() {
        AnalyzeRequestDto request = requestDto();

        when(analyzeService.analyze(any(), eq(null))).thenReturn(Result.ok(sampleResult()));

        ResponseEntity<?> response = analyzeController.analyze(request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(analyzeService).analyze(any(AnalyzeRequest.class), eq(null));
    }

    @Test
    public void analyze_authenticated_user_uses_user_id_from_principal() {
        AnalyzeRequestDto request = requestDto();
        UUID userId = UUID.randomUUID();

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("userId", userId.toString()));
        when(analyzeService.analyze(any(), eq(userId))).thenReturn(Result.ok(sampleResult()));

        ResponseEntity<?> response = analyzeController.analyze(request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<AnalyzeRequest> captor = ArgumentCaptor.forClass(AnalyzeRequest.class);
        verify(analyzeService).analyze(captor.capture(), eq(userId));
        assertEquals(TimeResolution.M15, captor.getValue().resolution());
        assertEquals(new BigDecimal("25.5"), captor.getValue().positionPct());
    }

    @Test
    public void analyze_invalid_input_maps_bad_request() {
        AnalyzeRequestDto request = requestDto();
        when(analyzeService.analyze(any(), eq(null)))
                .thenReturn(Result.err(new AnalyzeFailure.InvalidInput("bad input")));

        ResponseEntity<?> response = analyzeController.analyze(request, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
    }

    private AnalyzeRequestDto requestDto() {
        return new AnalyzeRequestDto(
                app.leesh.tratic.chart.domain.Market.BINANCE,
                "BTCUSDT",
                TimeResolution.M15,
                Instant.parse("2026-01-10T10:00:00Z"),
                new BigDecimal("100"),
                new BigDecimal("90"),
                new BigDecimal("110"),
                new BigDecimal("25.5"));
    }

    private AnalyzeResult sampleResult() {
        return new AnalyzeResult(
                AnalyzeDirection.LONG,
                10.0,
                20.0,
                VolatilityLabel.MID,
                30.0,
                40.0,
                0.2,
                0.1);
    }
}
