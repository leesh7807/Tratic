package app.leesh.tratic.chart.infra.upbit;

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

class UpbitRateLimiterTest {

    @Test
    void acquire_returnsInvalidRequestWhenRequestCountOutOfRange() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        RecordingSleeper sleeper = new RecordingSleeper();
        UpbitRateLimiter limiter = new UpbitRateLimiter(clock, sleeper);

        Result<Void, ChartFetchFailure> zero = limiter.acquire(0);
        Result<Void, ChartFetchFailure> overTen = limiter.acquire(11);

        assertInstanceOf(Result.Err.class, zero);
        assertInstanceOf(Result.Err.class, overTen);
        assertInstanceOf(ChartFetchFailure.InvalidRequest.class, ((Result.Err<Void, ChartFetchFailure>) zero).error());
        assertInstanceOf(ChartFetchFailure.InvalidRequest.class,
                ((Result.Err<Void, ChartFetchFailure>) overTen).error());
    }

    @Test
    void acquire_sleepsWhenWindowIsExhausted() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        RecordingSleeper sleeper = new RecordingSleeper(clock);
        UpbitRateLimiter limiter = new UpbitRateLimiter(clock, sleeper);

        assertInstanceOf(Result.Ok.class, limiter.acquire(10));

        clock.setInstant(Instant.parse("2026-01-01T00:00:00.400Z"));
        Result<Void, ChartFetchFailure> result = limiter.acquire(1);

        assertInstanceOf(Result.Ok.class, result);
        assertEquals(Duration.ofMillis(600), sleeper.totalSlept());
    }

    @Test
    void syncRemainingReqHeader_updatesLocalWindowUsage() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        RecordingSleeper sleeper = new RecordingSleeper(clock);
        UpbitRateLimiter limiter = new UpbitRateLimiter(clock, sleeper);

        limiter.syncRemainingReqHeader("group=market; min=599; sec=3");
        clock.setInstant(Instant.parse("2026-01-01T00:00:00.500Z"));

        Result<Void, ChartFetchFailure> result = limiter.acquire(4);
        assertInstanceOf(Result.Ok.class, result);
        assertEquals(Duration.ofMillis(500), sleeper.totalSlept());
    }

    @Test
    void syncRemainingReqHeader_ignoresMalformedValue() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        RecordingSleeper sleeper = new RecordingSleeper(clock);
        UpbitRateLimiter limiter = new UpbitRateLimiter(clock, sleeper);

        limiter.syncRemainingReqHeader("group=market; min=599");
        Result<Void, ChartFetchFailure> result = limiter.acquire(10);

        assertInstanceOf(Result.Ok.class, result);
        clock.setInstant(Instant.parse("2026-01-01T00:00:00.500Z"));
        Result<Void, ChartFetchFailure> exceed = limiter.acquire(1);
        assertInstanceOf(Result.Ok.class, exceed);
        assertTrue(sleeper.totalSlept().compareTo(Duration.ZERO) > 0);
    }

    @Test
    void acquire_returnsRateLimitedWhenInterrupted() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        InterruptingSleeper sleeper = new InterruptingSleeper();
        UpbitRateLimiter limiter = new UpbitRateLimiter(clock, sleeper);

        assertInstanceOf(Result.Ok.class, limiter.acquire(10));
        clock.setInstant(Instant.parse("2026-01-01T00:00:00.900Z"));

        Result<Void, ChartFetchFailure> result = limiter.acquire(1);
        assertInstanceOf(Result.Err.class, result);

        Result.Err<Void, ChartFetchFailure> err = (Result.Err<Void, ChartFetchFailure>) result;
        assertInstanceOf(ChartFetchFailure.RateLimited.class, err.error());
        ChartFetchFailure.RateLimited rateLimited = (ChartFetchFailure.RateLimited) err.error();
        assertEquals(Market.UPBIT, rateLimited.market());
        assertEquals(Duration.ofMillis(100), rateLimited.retryAfter());
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
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

    private static final class InterruptingSleeper implements Sleeper {
        @Override
        public void sleep(Duration duration) throws InterruptedException {
            throw new InterruptedException("interrupted in test");
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
