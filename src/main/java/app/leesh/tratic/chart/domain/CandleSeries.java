package app.leesh.tratic.chart.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class CandleSeries {
    private final List<Candle> candles;

    private CandleSeries(List<Candle> candles) {
        this.candles = List.copyOf(candles);
    }

    /**
     * 정렬/중복 규칙을 만족하는 캔들 시리즈를 생성한다.
     * 시간 오름차순이며 동일 시각 캔들이 없어야 한다.
     */
    public static CandleSeries ofSorted(List<Candle> candles) {
        Objects.requireNonNull(candles, "candles must not be null");

        List<Instant> times = candles.stream().map(Candle::time).toList();

        Set<Instant> unique = new HashSet<>(times);
        if (times.size() != unique.size()) {
            throw new IllegalArgumentException("Duplicated time candle exist");
        }

        if (!IntStream.range(0, times.size() - 1)
                .allMatch(i -> times.get(i).compareTo(times.get(i + 1)) < 0)) {
            throw new IllegalArgumentException("Not sorted candles");
        }

        return new CandleSeries(candles);
    }

    /**
     * 시리즈 내 캔들의 시간에 대해 Consumer 연산
     * 
     * @param consumer 한 개의 입력 값을 받아 리턴하지 않는 함수형 인터페이스
     */
    public void forEachTime(Consumer<Instant> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        for (Candle c : candles) {
            consumer.accept(c.time());
        }
    }

    /**
     * 시리즈 길이를 반환한다.
     */
    public int size() {
        return candles.size();
    }

    /**
     * 시리즈가 비어있는지 반환한다.
     */
    public boolean isEmpty() {
        return candles.isEmpty();
    }

    /**
     * 주어진 시점이 속한 버킷의 시작 시각 이전 캔들만 반환한다.
     * 분석 시 현재 버킷 제외(look-ahead 방지) 용도로 사용한다.
     */
    public List<Candle> candlesBeforeBucketOf(Instant timePoint, Duration bucketDuration) {
        Objects.requireNonNull(timePoint, "timePoint must not be null");
        Objects.requireNonNull(bucketDuration, "bucketDuration must not be null");

        long bucketMillis = bucketDuration.toMillis();
        if (bucketMillis <= 0L) {
            throw new IllegalArgumentException("bucketDuration must be positive");
        }

        long bucketStartMillis = Math.floorDiv(timePoint.toEpochMilli(), bucketMillis) * bucketMillis;
        Instant bucketStart = Instant.ofEpochMilli(bucketStartMillis);

        return candles.stream()
                .filter(candle -> candle.time().isBefore(bucketStart))
                .toList();
    }
}
