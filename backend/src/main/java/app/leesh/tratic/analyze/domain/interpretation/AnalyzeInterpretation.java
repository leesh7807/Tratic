package app.leesh.tratic.analyze.domain.interpretation;

import java.util.Objects;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;

public record AnalyzeInterpretation(
        AnalyzeDirection direction,
        AnalyzeScenario scenario,
        String bias,
        String confidence,
        String riskLevel,
        String policyVersion) {

    public AnalyzeInterpretation {
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(bias, "bias must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        Objects.requireNonNull(policyVersion, "policyVersion must not be null");
    }
}
