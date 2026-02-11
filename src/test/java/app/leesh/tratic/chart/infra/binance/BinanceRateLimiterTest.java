package app.leesh.tratic.chart.infra.binance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;
import app.leesh.tratic.shared.time.Sleeper;

class BinanceRateLimiterTest {

    @Test
    void calculateWeight_matchesRequiredRanges() {
        assertEquals(1, BinanceApiClient.calculateWeight(1));
        assertEquals(1, BinanceApiClient.calculateWeight(99));
        assertEquals(2, BinanceApiClient.calculateWeight(100));
        assertEquals(2, BinanceApiClient.calculateWeight(499));
        assertEquals(5, BinanceApiClient.calculateWeight(500));
        assertEquals(5, BinanceApiClient.calculateWeight(1000));
        assertEquals(10, BinanceApiClient.calculateWeight(1001));
        assertEquals(10, BinanceApiClient.calculateWeight(1500));
    }

    @Test
    void acquire_fastFailsWhenExpectedWaitExceedsThreeSeconds() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        RecordingSleeper sleeper = new RecordingSleeper();
        BinanceRateLimiter limiter = new BinanceRateLimiter(clock, sleeper);

        assertInstanceOf(Result.Ok.class, limiter.acquire(6000));

        clock.setInstant(Instant.parse("2026-01-01T00:00:01Z"));
        Result<Void, ChartFetchFailure> result = limiter.acquire(10);

        assertInstanceOf(Result.Err.class, result);
        Result.Err<Void, ChartFetchFailure> err = (Result.Err<Void, ChartFetchFailure>) result;
        assertInstanceOf(ChartFetchFailure.RateLimited.class, err.error());
        ChartFetchFailure.RateLimited rateLimited = (ChartFetchFailure.RateLimited) err.error();
        assertEquals(Market.BINANCE, err.error().market());
        assertEquals(Duration.ofSeconds(59), rateLimited.retryAfter());
        assertTrue(sleeper.totalSlept().isZero());
    }

    @Test
    void acquire_sleepsWhenExpectedWaitIsThreeSecondsOrLess() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        RecordingSleeper sleeper = new RecordingSleeper(clock);
        BinanceRateLimiter limiter = new BinanceRateLimiter(clock, sleeper);

        assertInstanceOf(Result.Ok.class, limiter.acquire(6000));

        clock.setInstant(Instant.parse("2026-01-01T00:00:57Z"));
        Result<Void, ChartFetchFailure> result = limiter.acquire(10);

        assertInstanceOf(Result.Ok.class, result);
        assertEquals(Duration.ofSeconds(3), sleeper.totalSlept());
    }

    @Test
    void acquire_returnsInvalidRequestWhenWeightIsOutOfRange() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        RecordingSleeper sleeper = new RecordingSleeper();
        BinanceRateLimiter limiter = new BinanceRateLimiter(clock, sleeper);

        Result<Void, ChartFetchFailure> nonPositive = limiter.acquire(0);
        Result<Void, ChartFetchFailure> tooLarge = limiter.acquire(6001);

        assertInstanceOf(Result.Err.class, nonPositive);
        assertInstanceOf(Result.Err.class, tooLarge);

        Result.Err<Void, ChartFetchFailure> nonPositiveErr = (Result.Err<Void, ChartFetchFailure>) nonPositive;
        Result.Err<Void, ChartFetchFailure> tooLargeErr = (Result.Err<Void, ChartFetchFailure>) tooLarge;
        assertInstanceOf(ChartFetchFailure.InvalidRequest.class, nonPositiveErr.error());
        assertInstanceOf(ChartFetchFailure.InvalidRequest.class, tooLargeErr.error());
    }

    private static final class RecordingSleeper implements Sleeper {
        private Duration slept = Duration.ZERO;
        private final MutableClock clock;

        private RecordingSleeper() {
            this.clock = null;
        }

        private RecordingSleeper(MutableClock clock) {
            this.clock = clock;
        }

        @Override
        public void sleep(Duration duration) {
            slept = slept.plus(duration);
            if (clock != null) {
                clock.setInstant(clock.instant().plus(duration));
            }
        }

        private Duration totalSlept() {
            return slept;
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant initial) {
            this.instant = initial;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
