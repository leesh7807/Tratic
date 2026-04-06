package app.leesh.tratic.analyze.infra.config;

import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AnalyzeClassificationProps(
        @NotNull @Valid ScoreClassificationRangesProps trend,
        @NotNull @Valid ScoreClassificationRangesProps location,
        @NotNull @Valid ScoreClassificationRangesProps pressure) {

    public AnalyzeClassificationProps {
        Objects.requireNonNull(trend, "trend must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(pressure, "pressure must not be null");
    }
}
