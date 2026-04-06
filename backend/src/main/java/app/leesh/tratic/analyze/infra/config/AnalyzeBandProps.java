package app.leesh.tratic.analyze.infra.config;

import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AnalyzeBandProps(
        @NotNull @Valid AnalyzeBandSetProps trend,
        @NotNull @Valid AnalyzeBandSetProps location,
        @NotNull @Valid AnalyzeBandSetProps pressure) {

    public AnalyzeBandProps {
        Objects.requireNonNull(trend, "trend must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(pressure, "pressure must not be null");
    }
}
