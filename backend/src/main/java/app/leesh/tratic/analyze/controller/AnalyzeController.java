package app.leesh.tratic.analyze.controller;

import java.util.Map;
import java.util.Objects;
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

import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;
import app.leesh.tratic.analyze.service.AnalyzeInterpretationRenderer;
import app.leesh.tratic.analyze.service.AnalyzeRequest;
import app.leesh.tratic.analyze.service.AnalyzeService;
import app.leesh.tratic.analyze.service.error.AnalyzeFailure;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/analyze")
@Validated
public class AnalyzeController {
    private final AnalyzeService analyzeService;
    private final AnalyzeInterpretationRenderer interpretationRenderer;

    public AnalyzeController(AnalyzeService analyzeService, AnalyzeInterpretationRenderer interpretationRenderer) {
        this.analyzeService = analyzeService;
        this.interpretationRenderer = interpretationRenderer;
    }

    @PostMapping
    @Operation(summary = "Analyze a trade setup from chart data")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analysis completed",
                    content = @Content(schema = @Schema(implementation = AnalyzeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid analyze input or chart request"),
            @ApiResponse(responseCode = "404", description = "Chart not found"),
            @ApiResponse(responseCode = "422", description = "Not enough candles to analyze"),
            @ApiResponse(responseCode = "502", description = "Chart provider unauthorized"),
            @ApiResponse(responseCode = "503", description = "Chart provider temporary failure or rate limit")
    })
    public ResponseEntity<?> analyze(
            @Valid @RequestBody AnalyzeRequestDto request,
            @Parameter(hidden = true) Authentication authentication) {
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
        return analyzeService.analyze(analyzeRequest, authenticatedUserId)
                .map(this::toResponseDto)
                .mapError(this::toApiError)
                .fold(
                        ResponseEntity::ok,
                        this::toErrorResponse);
    }

    private AnalyzeResponseDto toResponseDto(AnalyzeInterpretation value) {
        return new AnalyzeResponseDto(
                value.direction(),
                value.scenario(),
                interpretationRenderer.render(value),
                value.bias(),
                value.confidence(),
                value.riskLevel());
    }

    private ApiError toApiError(AnalyzeFailure failure) {
        if (failure instanceof AnalyzeFailure.InvalidInput invalidInput) {
            return new ApiError(HttpStatus.BAD_REQUEST, Map.of("message", invalidInput.message()));
        }
        if (failure instanceof AnalyzeFailure.InsufficientCandles insufficient) {
            return new ApiError(HttpStatus.UNPROCESSABLE_ENTITY, Map.of(
                    "message", "not enough candles to analyze",
                    "required", String.valueOf(insufficient.required()),
                    "actual", String.valueOf(insufficient.actual())));
        }

        ChartFetchFailure cause = ((AnalyzeFailure.ChartDataUnavailable) failure).cause();
        if (cause instanceof ChartFetchFailure.InvalidRequest) {
            return new ApiError(HttpStatus.BAD_REQUEST, Map.of("message", "invalid chart request"));
        }
        if (cause instanceof ChartFetchFailure.NotFound) {
            return new ApiError(HttpStatus.NOT_FOUND, Map.of("message", "chart not found"));
        }
        if (cause instanceof ChartFetchFailure.RateLimited) {
            return new ApiError(HttpStatus.SERVICE_UNAVAILABLE, Map.of("message", "chart rate limited"));
        }
        if (cause instanceof ChartFetchFailure.Unauthorized) {
            return new ApiError(HttpStatus.BAD_GATEWAY, Map.of("message", "chart unauthorized"));
        }

        return new ApiError(HttpStatus.SERVICE_UNAVAILABLE, Map.of("message", "chart temporary failure"));
    }

    private ResponseEntity<Map<String, String>> toErrorResponse(ApiError apiError) {
        return ResponseEntity.status(apiError.status().value()).body(apiError.body());
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

    private record ApiError(HttpStatus status, Map<String, String> body) {
        private ApiError {
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(body, "body must not be null");
        }
    }
}
