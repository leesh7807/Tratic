package app.leesh.tratic.analyze.domain.classification;

import java.util.List;

public record ScoreClassificationRanges<E extends Enum<E>>(
        List<ScoreClassificationRange<E>> ranges) {

    public ScoreClassificationRanges {
        if (ranges == null || ranges.isEmpty()) {
            throw new IllegalArgumentException("ranges must not be empty");
        }
        ranges = List.copyOf(ranges);
    }

    public E resolve(double score) {
        return ranges.stream()
                .filter(range -> range.matches(score))
                .findFirst()
                .map(ScoreClassificationRange::code)
                .orElseThrow(() -> new IllegalArgumentException("no classification configured for score=" + score));
    }

    public double minimumScore() {
        return ranges.stream()
                .mapToDouble(ScoreClassificationRange::minInclusive)
                .min()
                .orElseThrow(() -> new IllegalArgumentException("ranges must not be empty"));
    }

    public double maximumScore() {
        double maxExclusive = ranges.stream()
                .mapToDouble(ScoreClassificationRange::maxExclusive)
                .max()
                .orElseThrow(() -> new IllegalArgumentException("ranges must not be empty"));
        return Math.nextDown(maxExclusive);
    }
}
