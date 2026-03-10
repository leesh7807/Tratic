package app.leesh.tratic.chart.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

public class CandleTest {
        @Test
        public void 캔들값들_비어있으면_안됨() {
                assertThrows(NullPointerException.class,
                                () -> new Candle(null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                                                BigDecimal.ONE));
                assertThrows(NullPointerException.class,
                                () -> new Candle(Instant.now(), null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                                                BigDecimal.ONE));
                assertThrows(NullPointerException.class,
                                () -> new Candle(Instant.now(), BigDecimal.ONE, null, BigDecimal.ONE, BigDecimal.ONE,
                                                BigDecimal.ONE));
                assertThrows(NullPointerException.class,
                                () -> new Candle(Instant.now(), BigDecimal.ONE, BigDecimal.ONE, null, BigDecimal.ONE,
                                                BigDecimal.ONE));
                assertThrows(NullPointerException.class,
                                () -> new Candle(Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null,
                                                BigDecimal.ONE));
                assertThrows(NullPointerException.class,
                                () -> new Candle(Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                                                BigDecimal.ONE, null));
        }

        @Test
        public void 고가와_저가_역전되면_안됨() {
                BigDecimal low = BigDecimal.TEN;
                BigDecimal high = low.subtract(BigDecimal.ONE);

                assertThrows(IllegalArgumentException.class,
                                () -> new Candle(Instant.now(), low, high, low, BigDecimal.ONE, BigDecimal.ONE));
        }

        @Test
        public void 시가와종가_고가와저가_사이여야함() {
                BigDecimal low = BigDecimal.ONE;
                BigDecimal high = BigDecimal.TEN;

                assertThrows(IllegalArgumentException.class,
                                () -> new Candle(Instant.now(), low.subtract(BigDecimal.ONE), high, low, low,
                                                BigDecimal.ONE));
                assertThrows(IllegalArgumentException.class,
                                () -> new Candle(Instant.now(), low, high, low, high.add(BigDecimal.ONE),
                                                BigDecimal.ONE));
        }
}
