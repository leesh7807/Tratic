package app.leesh.tratic.analyze.domain;

import java.util.Objects;

import app.leesh.tratic.analyze.domain.classification.AnalyzeClassificationSpec;

public record AnalyzeSpec(
        AnalyzeEngineParams engineParams,
        AnalyzeClassificationSpec classificationSpec) {

    public AnalyzeSpec {
        Objects.requireNonNull(engineParams, "engineParams must not be null");
        Objects.requireNonNull(classificationSpec, "classificationSpec must not be null");
    }
}
