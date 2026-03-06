package app.leesh.tratic.analyze.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.service.AnalyzeRequest;
import app.leesh.tratic.analyze.service.AnalyzeService;
import app.leesh.tratic.analyze.service.error.AnalyzeFailure;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/analyze")
@Validated
public class AnalyzeController {
    private final AnalyzeService analyzeService;

    public AnalyzeController(AnalyzeService analyzeService) {
        this.analyzeService = analyzeService;
    }

    @PostMapping
    public ResponseEntity<?> analyze(@Valid @RequestBody AnalyzeRequestDto request, Authentication authentication) {
        AnalyzeRequest analyzeRequest = new AnalyzeRequest(
                request.market(),
                request.symbol(),
                request.resolution(),
                request.entryAt(),
                request.entryPrice(),
                request.stopLossPrice(),
                request.takeProfitPrice(),
                request.positionPct());

        UUID authenticatedUserId = resolveUserId(authentication);
        Result<AnalyzeResult, AnalyzeFailure> result = analyzeService.analyze(analyzeRequest, authenticatedUserId);

        if (result instanceof Result.Ok<AnalyzeResult, AnalyzeFailure> ok) {
            AnalyzeResult value = ok.value();
            return ResponseEntity.ok(new AnalyzeResponseDto(
                    value.direction(),
                    value.trendScore(),
                    value.volatilityScore(),
                    value.volatilityLabel(),
                    value.locationScore(),
                    value.pressureScore(),
                    value.pressureRaw(),
                    value.pressureView()));
        }

        AnalyzeFailure failure = ((Result.Err<AnalyzeResult, AnalyzeFailure>) result).error();
        return mapFailure(failure);
    }

    private ResponseEntity<Map<String, String>> mapFailure(AnalyzeFailure failure) {
        if (failure instanceof AnalyzeFailure.InvalidInput invalidInput) {
            return ResponseEntity.badRequest().body(Map.of("message", invalidInput.message()));
        }
        if (failure instanceof AnalyzeFailure.InsufficientCandles insufficient) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "message", "not enough candles to analyze",
                    "required", String.valueOf(insufficient.required()),
                    "actual", String.valueOf(insufficient.actual())));
        }

        ChartFetchFailure cause = ((AnalyzeFailure.ChartDataUnavailable) failure).cause();
        if (cause instanceof ChartFetchFailure.InvalidRequest) {
            return ResponseEntity.badRequest().body(Map.of("message", "invalid chart request"));
        }
        if (cause instanceof ChartFetchFailure.NotFound) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "chart not found"));
        }
        if (cause instanceof ChartFetchFailure.RateLimited) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "chart rate limited"));
        }
        if (cause instanceof ChartFetchFailure.Unauthorized) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "chart unauthorized"));
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "chart temporary failure"));
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oAuth2User) {
            Object raw = oAuth2User.getAttributes().get("userId");
            if (raw instanceof String value) {
                try {
                    return UUID.fromString(value);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }

        return null;
    }
}
