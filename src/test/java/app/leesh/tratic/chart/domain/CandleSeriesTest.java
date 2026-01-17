package app.leesh.tratic.chart.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class CandleSeriesTest {
    Instant current = Instant.now();
    Instant past = current.minus(5, ChronoUnit.MINUTES);
    Instant future = current.plus(5, ChronoUnit.MINUTES);

    @Test
    public void NULL_안됨() {
        assertThrows(NullPointerException.class, () -> CandleSeries.ofSorted(null));
    }

    @Test
    public void 정렬된_데이터여야함() {
        assertThrows(IllegalArgumentException.class, () -> makeTempCandleSeries(future, current, past));
    }

    @Test
    public void 중복된_시간데이터_없어야함() {
        Instant duplicated = current;

        assertThrows(IllegalArgumentException.class, () -> makeTempCandleSeries(duplicated, current, future));
    }

    public CandleSeries makeTempCandleSeries(Instant... times) {
        return CandleSeries.ofSorted(Stream.of(times).map(t -> makeTempCandle(t)).toList());
    }

    public Candle makeTempCandle(Instant time) {
        return new Candle(time, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    }
}
