package app.leesh.tratic.analyze.controller;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnalyzeResponseDto(
        @Schema(description = "Trend band in Korean", example = "상승")
        String trend,
        @Schema(description = "Location band in Korean", example = "상단")
        String location,
        @Schema(description = "Pressure band in Korean", example = "매수 우세")
        String pressure) {
}
