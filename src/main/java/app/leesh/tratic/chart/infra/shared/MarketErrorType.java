package app.leesh.tratic.chart.infra.shared;

import java.time.Duration;

public enum MarketErrorType {
    TEMPORARY,
    RATE_LIMITED,
    INVALID_REQUEST,
    UNAUTHORIZED,
    NOT_FOUND;

    public MarketException exception(int httpStatus, String rawMessage, Duration retryAfter) {
        return new MarketException(this, httpStatus, rawMessage, retryAfter);
    }
}
