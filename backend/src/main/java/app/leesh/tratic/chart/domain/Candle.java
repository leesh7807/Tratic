package app.leesh.tratic.chart.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Candle(
        Instant time,
        BigDecimal o,
        BigDecimal h,
        BigDecimal l,
        BigDecimal c,
        BigDecimal v) {

    public Candle {
        Objects.requireNonNull(time, "time must not be null");
        Objects.requireNonNull(o, "open must not be null");
        Objects.requireNonNull(h, "high must not be null");
        Objects.requireNonNull(l, "low must not be null");
        Objects.requireNonNull(c, "close must not be null");
        Objects.requireNonNull(v, "volume must not be null");

        if (l.compareTo(h) > 0) {
            throw new IllegalArgumentException("low must lower than high");
        }

        if (o.compareTo(l) < 0 || o.compareTo(h) > 0) {
            throw new IllegalArgumentException("open and close must be between low and high");
        }
        if (c.compareTo(l) < 0 || c.compareTo(h) > 0) {
            throw new IllegalArgumentException("open and close must be between low and high");
        }
    }
}
