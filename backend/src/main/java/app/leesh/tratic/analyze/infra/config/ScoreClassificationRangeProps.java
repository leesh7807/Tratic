package app.leesh.tratic.analyze.infra.config;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

public record ScoreClassificationRangeProps(
        @NotNull String code,
        double minInclusive,
        double maxExclusive) {

    public ScoreClassificationRangeProps {
        Objects.requireNonNull(code, "code must not be null");
        if (Double.isNaN(minInclusive) || Double.isNaN(maxExclusive) || minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("classification range must satisfy minInclusive < maxExclusive");
        }
    }
}
