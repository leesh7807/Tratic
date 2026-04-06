package app.leesh.tratic.analyze.controller;

import app.leesh.tratic.analyze.domain.classification.ClassifiedAnalyzeResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record AnalyzeResponseDto(
        @Schema(description = "Trend band in Korean", example = "상승")
        String trend,
        @Schema(description = "Location band in Korean", example = "상단")
        String location,
        @Schema(description = "Pressure band in Korean", example = "매수 우세")
        String pressure) {

    public static AnalyzeResponseDto from(ClassifiedAnalyzeResult classified) {
        return new AnalyzeResponseDto(
                classified.trend().displayKo(),
                classified.location().displayKo(),
                classified.pressure().displayKo());
    }
}
