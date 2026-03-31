package app.leesh.tratic.analyze.domain.interpretation;

import java.util.Objects;

public record AnalyzeSignalLabels(
        String trend,
        String volatility,
        String location,
        String pressure) {

    public AnalyzeSignalLabels {
        Objects.requireNonNull(trend, "trend must not be null");
        Objects.requireNonNull(volatility, "volatility must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(pressure, "pressure must not be null");
    }
}
