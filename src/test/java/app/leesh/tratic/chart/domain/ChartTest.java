package app.leesh.tratic.chart.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class ChartTest {
    Market market = Market.UPBIT;
    Symbol symbol = new Symbol("BTC");
    TimeResolution timeResolution = TimeResolution.D1;
    ChartSignature sig = new ChartSignature(market, symbol, timeResolution);

    @Test
    public void 차트는_해상도에_맞는_캔들시리즈를_가져야함() {
        Instant time = Instant.parse("2025-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> Chart.of(sig, makeTempCandleSeries(time, time.plus(1, ChronoUnit.HOURS))));
    }

    @Test
    public void 차트가_해상도에_맞는_캔들시리즈_가졌을_때() {
        Instant time = Instant.parse("2025-01-01T00:00:00Z");
        assertDoesNotThrow(() -> Chart.of(sig, makeTempCandleSeries(time, time.plus(1, ChronoUnit.DAYS))));
    }

    @Test
    public void 분석시_현재_버킷_캔들은_제외함() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = t1.plus(1, ChronoUnit.DAYS);
        Instant t3 = t2.plus(1, ChronoUnit.DAYS);
        Chart chart = Chart.of(sig, makeTempCandleSeries(t1, t2, t3));

        List<Candle> candles = chart.candlesForAnalysisAt(t3.plus(1, ChronoUnit.HOURS));

        assertEquals(List.of(t1, t2), candles.stream().map(Candle::time).toList());
    }

    public CandleSeries makeTempCandleSeries(Instant... times) {
        return CandleSeries.ofSorted(Stream.of(times).map(t -> makeTempCandle(t)).toList());
    }

    public Candle makeTempCandle(Instant time) {
        return new Candle(time, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    }
}
