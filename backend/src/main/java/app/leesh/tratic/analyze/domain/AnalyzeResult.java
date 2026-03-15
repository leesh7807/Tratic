package app.leesh.tratic.analyze.domain;

public record AnalyzeResult(
        AnalyzeDirection direction,
        double trendScore,
        double volatilityScore,
        double locationScore,
        double pressureScore,
        double pressureRaw,
        double pressureView) {
}
