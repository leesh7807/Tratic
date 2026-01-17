package app.leesh.tratic.chart.domain;

import java.time.Duration;

public enum TimeResolution {
    M1(Duration.ofMinutes(1)),
    M3(Duration.ofMinutes(3)),
    M5(Duration.ofMinutes(5)),
    M15(Duration.ofMinutes(15)),
    M30(Duration.ofMinutes(30)),
    H1(Duration.ofHours(1)),
    H4(Duration.ofHours(4)),
    D1(Duration.ofDays(1));

    private final Duration duration;

    TimeResolution(Duration duration) {
        this.duration = duration;
    }

    /** 이 해상도가 의미하는 시간 길이 */
    public Duration toDuration() {
        return duration;
    }
}
