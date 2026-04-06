package app.leesh.tratic.analyze.domain;

public record AnalyzeResult(
        AnalyzeDirection direction,
        double trendScore,
        double locationScore,
        double pressureScore) {
}
