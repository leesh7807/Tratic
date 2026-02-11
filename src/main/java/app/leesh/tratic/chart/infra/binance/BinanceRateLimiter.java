package app.leesh.tratic.chart.infra.binance;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;
import app.leesh.tratic.shared.time.Sleeper;

@Component
public class BinanceRateLimiter {

    private static final int MAX_WEIGHT_PER_MINUTE = 6000;
    private static final Duration WINDOW_SIZE = Duration.ofMinutes(1);
    private static final Duration FAST_FAIL_THRESHOLD = Duration.ofSeconds(3);

    private final Clock clock;
    private final Sleeper sleeper;

    private Instant windowStart = Instant.EPOCH;
    private int usedWeightInWindow = 0;

    public BinanceRateLimiter(Clock clock, Sleeper sleeper) {
        this.clock = clock;
        this.sleeper = sleeper;
    }

    public synchronized Result<Void, ChartFetchFailure> acquire(int requestWeight) {
        if (requestWeight <= 0 || requestWeight > MAX_WEIGHT_PER_MINUTE) {
            return Result.err(new ChartFetchFailure.InvalidRequest(Market.BINANCE));
        }

        Instant now = clock.instant();
        rotateWindowIfNeeded(now);

        if (usedWeightInWindow + requestWeight <= MAX_WEIGHT_PER_MINUTE) {
            usedWeightInWindow += requestWeight;
            return Result.ok(null);
        }

        Duration retryAfter = Duration.between(now, windowStart.plus(WINDOW_SIZE));
        if (retryAfter.isNegative()) {
            retryAfter = Duration.ZERO;
        }

        if (retryAfter.compareTo(FAST_FAIL_THRESHOLD) > 0) {
            return Result.err(new ChartFetchFailure.RateLimited(Market.BINANCE, retryAfter));
        }

        try {
            sleeper.sleep(retryAfter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.err(new ChartFetchFailure.RateLimited(Market.BINANCE, retryAfter));
        }

        now = clock.instant();
        rotateWindowIfNeeded(now);
        usedWeightInWindow += requestWeight;
        return Result.ok(null);
    }

    private void rotateWindowIfNeeded(Instant now) {
        if (!now.isBefore(windowStart.plus(WINDOW_SIZE))) {
            windowStart = now;
            usedWeightInWindow = 0;
        }
    }
}
