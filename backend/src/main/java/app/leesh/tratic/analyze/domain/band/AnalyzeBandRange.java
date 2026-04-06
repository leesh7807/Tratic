package app.leesh.tratic.analyze.domain.band;

public record AnalyzeBandRange<E extends Enum<E>>(
        E code,
        double minInclusive,
        double maxExclusive) {

    public AnalyzeBandRange {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        if (Double.isNaN(minInclusive) || Double.isNaN(maxExclusive) || minInclusive >= maxExclusive) {
            throw new IllegalArgumentException("band range must satisfy minInclusive < maxExclusive");
        }
    }

    public boolean matches(double score) {
        return score >= minInclusive && score < maxExclusive;
    }
}
