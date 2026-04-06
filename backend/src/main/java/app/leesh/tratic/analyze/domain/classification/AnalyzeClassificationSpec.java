package app.leesh.tratic.analyze.domain.classification;

import app.leesh.tratic.analyze.domain.AnalyzeResult;

public record AnalyzeClassificationSpec(
        ScoreClassificationRanges<TrendBand> trend,
        ScoreClassificationRanges<LocationBand> location,
        ScoreClassificationRanges<PressureBand> pressure) {

    public AnalyzeClassificationSpec {
        if (trend == null || location == null || pressure == null) {
            throw new IllegalArgumentException("classification ranges must not be null");
        }
    }

    public ClassifiedAnalyzeResult classify(AnalyzeResult result) {
        return new ClassifiedAnalyzeResult(
                trend.resolve(result.trendScore()),
                location.resolve(result.locationScore()),
                pressure.resolve(result.pressureScore()));
    }
}
