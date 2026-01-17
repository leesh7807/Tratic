package app.leesh.tratic.chart.domain;

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

    public int size() {
        return candles.size();
    }

    public boolean isEmpty() {
        return candles.isEmpty();
    }
}
