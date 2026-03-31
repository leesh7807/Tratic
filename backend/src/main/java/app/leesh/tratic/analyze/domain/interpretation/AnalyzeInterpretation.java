package app.leesh.tratic.analyze.domain.interpretation;

import java.util.Objects;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;

public record AnalyzeInterpretation(
        AnalyzeDirection direction,
        AnalyzeScenario scenario,
        AnalyzeSignalLabels signals,
        String policyVersion) {

    public AnalyzeInterpretation {
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(signals, "signals must not be null");
        Objects.requireNonNull(policyVersion, "policyVersion must not be null");
    }
}
