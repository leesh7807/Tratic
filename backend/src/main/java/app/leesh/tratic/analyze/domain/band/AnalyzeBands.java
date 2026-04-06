package app.leesh.tratic.analyze.domain.band;

import java.util.Objects;

public record AnalyzeBands(
        TrendBand trend,
        LocationBand location,
        PressureBand pressure) {

    public AnalyzeBands {
        Objects.requireNonNull(trend, "trend must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(pressure, "pressure must not be null");
    }
}
