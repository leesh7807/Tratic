package app.leesh.tratic.analyze.domain.band;

import app.leesh.tratic.analyze.domain.AnalyzeResult;

public record AnalyzeBandSpec(
        AnalyzeBandSet<TrendBand> trend,
        AnalyzeBandSet<LocationBand> location,
        AnalyzeBandSet<PressureBand> pressure) {

    public AnalyzeBandSpec {
        if (trend == null || location == null || pressure == null) {
            throw new IllegalArgumentException("band sets must not be null");
        }
    }

    public AnalyzeBands resolve(AnalyzeResult result) {
        return new AnalyzeBands(
                trend.resolve(result.trendScore()),
                location.resolve(result.locationScore()),
                pressure.resolve(result.pressureScore()));
    }
}
