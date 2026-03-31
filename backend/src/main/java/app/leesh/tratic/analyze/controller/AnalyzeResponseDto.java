package app.leesh.tratic.analyze.controller;

import app.leesh.tratic.analyze.domain.interpretation.AnalyzeSignalLabels;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeScenario;
import io.swagger.v3.oas.annotations.media.Schema;

public record AnalyzeResponseDto(
        @Schema(description = "Stable interpretation scenario code", example = "BULLISH_TREND_CONTINUATION")
        AnalyzeScenario scenario,
        @Schema(description = "Holistic interpretation summary", example = "상승 추세와 매수 압력이 동행해 추세 지속 가능성이 높은 구간입니다.")
        String summary,
        @Schema(description = "Labeled signal snapshot used by the interpretation policy")
        AnalyzeSignalLabels signals) {
}
