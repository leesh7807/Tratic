package app.leesh.tratic.analyze.infra.config;

import java.util.Map;
import java.util.Objects;

import app.leesh.tratic.analyze.domain.AnalyzeEngineParams;
import app.leesh.tratic.chart.domain.TimeResolution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AnalyzeEngineProps(
        @NotNull @Valid AnalyzeEngineParams defaults,
        Map<TimeResolution, @Valid AnalyzeEngineParams> byResolution) {

    public AnalyzeEngineProps {
        Objects.requireNonNull(defaults, "defaults must not be null");
        byResolution = byResolution == null ? Map.of() : Map.copyOf(byResolution);
    }
}
