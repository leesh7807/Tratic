package app.leesh.tratic.analyze.controller;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeScenario;
import io.swagger.v3.oas.annotations.media.Schema;

public record AnalyzeResponseDto(
        @Schema(description = "Inferred trade direction", example = "LONG")
        AnalyzeDirection direction,
        @Schema(description = "Stable interpretation scenario code", example = "BULLISH_TREND_CONTINUATION")
        AnalyzeScenario scenario,
        @Schema(description = "Holistic interpretation summary", example = "상승 추세와 매수 압력이 동행해 추세 지속 가능성이 높은 구간입니다.")
        String summary,
        @Schema(description = "Overall bias of the setup", example = "CONTINUATION")
        String bias,
        @Schema(description = "Interpretation confidence", example = "HIGH")
        String confidence,
        @Schema(description = "Risk level implied by current context", example = "MEDIUM")
        String riskLevel) {
}
