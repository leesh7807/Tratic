package app.leesh.tratic.analyze.infra.config;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

public record AnalyzeBandRangeProps(
        @NotNull String code,
        double minInclusive,
        double maxExclusive) {

    public AnalyzeBandRangeProps {
        Objects.requireNonNull(code, "code must not be null");
        if (Double.isNaN(minInclusive) || Double.isNaN(maxExclusive) || minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("band range must satisfy minInclusive < maxExclusive");
        }
    }
}
