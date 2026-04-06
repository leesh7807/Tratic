package app.leesh.tratic.analyze.domain.classification;

import java.util.Objects;

public record ClassifiedAnalyzeResult(
        TrendBand trend,
        LocationBand location,
        PressureBand pressure) {

    public ClassifiedAnalyzeResult {
        Objects.requireNonNull(trend, "trend must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(pressure, "pressure must not be null");
    }
}
