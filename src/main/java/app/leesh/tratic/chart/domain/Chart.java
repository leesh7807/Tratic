package app.leesh.tratic.chart.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Chart {
    private final ChartSignature sig;
    private final CandleSeries candleSeries;

    private Chart(ChartSignature sig, CandleSeries candleSeries) {

        this.sig = sig;
        this.candleSeries = candleSeries;
    }

    /**
     * 차트 시그니처와 캔들 시리즈로 차트를 생성한다.
     * 시리즈 시각은 해상도 버킷 경계에 정렬되어야 한다.
     */
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

    /**
     * 차트 식별 정보(거래소/심볼/해상도)를 반환한다.
     */
    public ChartSignature chartSignature() {
        return this.sig;
    }

    /**
     * 분석 시점 기준으로 현재 버킷 이전 캔들만 반환한다.
     * 버킷 계산은 차트의 해상도를 기준으로 내부 위임 처리한다.
     */
    public List<Candle> candlesForAnalysisAt(Instant entryAt) {
        return candleSeries.candlesBeforeBucketOf(entryAt, sig.timeResolution().toDuration());
    }
}
