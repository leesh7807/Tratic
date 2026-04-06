package app.leesh.tratic.analyze.infra.config;

import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;

public record AnalyzeBandSetProps(
        List<@Valid AnalyzeBandRangeProps> ranges) {

    public AnalyzeBandSetProps {
        Objects.requireNonNull(ranges, "ranges must not be null");
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("ranges must not be empty");
        }
        ranges = List.copyOf(ranges);
    }
}
