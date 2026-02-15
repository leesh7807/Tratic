package app.leesh.tratic.chart.infra.upbit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.UpbitProps;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;
import app.leesh.tratic.shared.time.Sleeper;

@Component
public class UpbitRateLimiter {

    private static final int MAX_REQUESTS_PER_SECOND = 10;
    private static final Duration WINDOW_SIZE = Duration.ofSeconds(1);
    private static final Pattern SEC_PATTERN = Pattern.compile("sec\\s*=\\s*(\\d+)");

    private final Clock clock;
    private final Sleeper sleeper;
    private final Duration fastFailWaitThreshold;

    private Instant windowStart = Instant.EPOCH;
    private int usedRequestsInWindow = 0;

    public UpbitRateLimiter(Clock clock, Sleeper sleeper, UpbitProps props) {
        this.clock = clock;
        this.sleeper = sleeper;
        this.fastFailWaitThreshold = props.fastFailWaitThreshold();
        if (fastFailWaitThreshold.isNegative() || fastFailWaitThreshold.isZero()) {
            throw new IllegalArgumentException("clients.upbit.fast-fail-wait-threshold must be positive");
        }
    }

    public synchronized Result<Void, ChartFetchFailure> acquire(int requestCount) {
        if (requestCount <= 0 || requestCount > MAX_REQUESTS_PER_SECOND) {
            return Result.err(new ChartFetchFailure.InvalidRequest(Market.UPBIT));
        }

        Instant now = clock.instant();
        rotateWindowIfNeeded(now);

        if (usedRequestsInWindow + requestCount <= MAX_REQUESTS_PER_SECOND) {
            usedRequestsInWindow += requestCount;
            return Result.ok(null);
        }

        Duration retryAfter = Duration.between(now, windowStart.plus(WINDOW_SIZE));
        if (retryAfter.isNegative()) {
            retryAfter = Duration.ZERO;
        }

        if (retryAfter.compareTo(fastFailWaitThreshold) >= 0) {
            return Result.err(new ChartFetchFailure.RateLimited(Market.UPBIT, retryAfter));
        }

        try {
            sleeper.sleep(retryAfter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.err(new ChartFetchFailure.RateLimited(Market.UPBIT, retryAfter));
        }

        now = clock.instant();
        rotateWindowIfNeeded(now);
        usedRequestsInWindow += requestCount;
        return Result.ok(null);
    }

    public synchronized void syncRemainingReqHeader(String remainingReqHeaderValue) {
        if (remainingReqHeaderValue == null || remainingReqHeaderValue.isBlank()) {
            return;
        }

        Matcher matcher = SEC_PATTERN.matcher(remainingReqHeaderValue);
        if (!matcher.find()) {
            return;
        }

        int secRemaining;
        try {
            secRemaining = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return;
        }

        if (secRemaining < 0 || secRemaining > MAX_REQUESTS_PER_SECOND) {
            return;
        }

        Instant now = clock.instant();
        rotateWindowIfNeeded(now);
        int usedFromHeader = MAX_REQUESTS_PER_SECOND - secRemaining;
        usedRequestsInWindow = Math.max(usedRequestsInWindow, usedFromHeader);
    }

    private void rotateWindowIfNeeded(Instant now) {
        if (!now.isBefore(windowStart.plus(WINDOW_SIZE))) {
            windowStart = now;
            usedRequestsInWindow = 0;
        }
    }
}
