package app.leesh.tratic.analyze.domain;

import java.util.Objects;

import app.leesh.tratic.analyze.domain.band.AnalyzeBandSpec;

public record AnalyzeSpec(
        AnalyzeEngineParams engineParams,
        AnalyzeBandSpec bandSpec) {

    public AnalyzeSpec {
        Objects.requireNonNull(engineParams, "engineParams must not be null");
        Objects.requireNonNull(bandSpec, "bandSpec must not be null");
    }
}
