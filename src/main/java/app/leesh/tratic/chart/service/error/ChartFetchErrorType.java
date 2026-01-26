package app.leesh.tratic.chart.service.error;

import java.time.Duration;

public enum ChartFetchErrorType {
    TEMPORARY,
    RATE_LIMITED,
    INVALID_REQUEST,
    UNAUTHORIZED,
    NOT_FOUND;

    public ChartFetchException exception(Duration retryAfter) {
        return new ChartFetchException(this, retryAfter);
    }
}
