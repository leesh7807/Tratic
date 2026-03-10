package app.leesh.tratic.chart.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    @Test
    public void 특정_버킷_이전_캔들만_반환함() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = t1.plus(15, ChronoUnit.MINUTES);
        Instant t3 = t2.plus(15, ChronoUnit.MINUTES);
        CandleSeries series = makeTempCandleSeries(t1, t2, t3);

        List<Candle> candles = series.candlesBeforeBucketOf(t3.plus(1, ChronoUnit.MINUTES), Duration.ofMinutes(15));

        assertEquals(List.of(t1, t2), candles.stream().map(Candle::time).toList());
    }

    public CandleSeries makeTempCandleSeries(Instant... times) {
        return CandleSeries.ofSorted(Stream.of(times).map(t -> makeTempCandle(t)).toList());
    }

    public Candle makeTempCandle(Instant time) {
        return new Candle(time, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    }
}
