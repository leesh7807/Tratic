package app.leesh.tratic.chart.domain;

import java.time.Duration;
import java.time.Instant;

public class Chart {
    private final ChartSignature sig;
    private final CandleSeries candleSeries;

    private Chart(ChartSignature sig, CandleSeries candleSeries) {

        this.sig = sig;
        this.candleSeries = candleSeries;
    }

    public static Chart of(ChartSignature sig, CandleSeries candleSeries) {
        validateBucketAligned(sig.timeResolution(), candleSeries);
        return new Chart(sig, candleSeries);
    }

    private static void validateBucketAligned(TimeResolution resolution, CandleSeries series) {
        Duration frame = resolution.toDuration();
        long frameMillis = frame.toMillis();

        series.forEachTime((Instant ts) -> {
            long millis = ts.toEpochMilli();
            if (millis % frameMillis != 0) {
                throw new IllegalArgumentException(
                        "timestamp not aligned: ts=" + ts + ", resolution=" + resolution);
            }
        });
    }

    public ChartSignature chartSignature() {
        return this.sig;
    }
}
