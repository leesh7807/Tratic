package app.leesh.tratic.analyze.controller;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.VolatilityLabel;

public record AnalyzeResponseDto(
        AnalyzeDirection direction,
        double trendScore,
        double volatilityScore,
        VolatilityLabel volatilityLabel,
        double locationScore,
        double pressureScore,
        double pressureRaw,
        double pressureView) {
}
