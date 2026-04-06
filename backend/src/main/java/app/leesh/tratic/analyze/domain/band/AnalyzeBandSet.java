package app.leesh.tratic.analyze.domain.band;

import java.util.List;

public record AnalyzeBandSet<E extends Enum<E>>(
        List<AnalyzeBandRange<E>> ranges) {

    public AnalyzeBandSet {
        if (ranges == null || ranges.isEmpty()) {
            throw new IllegalArgumentException("ranges must not be empty");
        }
        ranges = List.copyOf(ranges);
    }

    public E resolve(double score) {
        return ranges.stream()
                .filter(range -> range.matches(score))
                .findFirst()
                .map(AnalyzeBandRange::code)
                .orElseThrow(() -> new IllegalArgumentException("no band configured for score=" + score));
    }

    public double minimumScore() {
        return ranges.stream()
                .mapToDouble(AnalyzeBandRange::minInclusive)
                .min()
                .orElseThrow(() -> new IllegalArgumentException("ranges must not be empty"));
    }

    public double maximumScore() {
        double maxExclusive = ranges.stream()
                .mapToDouble(AnalyzeBandRange::maxExclusive)
                .max()
                .orElseThrow(() -> new IllegalArgumentException("ranges must not be empty"));
        return Math.nextDown(maxExclusive);
    }
}
