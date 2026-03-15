package app.leesh.tratic.analyze.infra.interpretation;

import jakarta.validation.constraints.NotBlank;

public record MatrixAnalyzeScoreBand(
        @NotBlank String signal,
        double minInclusive,
        double maxExclusive) {

    public MatrixAnalyzeScoreBand {
        if (Double.isNaN(minInclusive) || Double.isNaN(maxExclusive) || minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("score band range must satisfy minInclusive < maxExclusive");
        }
    }

    public boolean matches(double score) {
        return score >= minInclusive && score < maxExclusive;
    }
}
