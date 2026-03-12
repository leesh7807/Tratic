package app.leesh.tratic.analyze.controller;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.VolatilityLabel;
import io.swagger.v3.oas.annotations.media.Schema;

public record AnalyzeResponseDto(
        @Schema(description = "Inferred trade direction", example = "LONG")
        AnalyzeDirection direction,
        @Schema(description = "Trend score", example = "10.0")
        double trendScore,
        @Schema(description = "Volatility score", example = "20.0")
        double volatilityScore,
        @Schema(description = "Volatility label", example = "MID")
        VolatilityLabel volatilityLabel,
        @Schema(description = "Location score in recent range", example = "30.0")
        double locationScore,
        @Schema(description = "Smoothed pressure score", example = "40.0")
        double pressureScore,
        @Schema(description = "Raw pressure value before smoothing", example = "0.2")
        double pressureRaw,
        @Schema(description = "Displayed pressure value after smoothing", example = "0.1")
        double pressureView) {
}
