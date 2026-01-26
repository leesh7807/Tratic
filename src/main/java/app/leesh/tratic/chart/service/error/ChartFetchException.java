package app.leesh.tratic.chart.service.error;

import java.time.Duration;

public final class ChartFetchException extends RuntimeException {

    private final ChartFetchErrorType type;
    private final Duration retryAfter;

    public ChartFetchException(ChartFetchErrorType type, Duration retryAfter) {
        super(buildMessage(type, retryAfter));
        this.type = type;
        this.retryAfter = retryAfter;
    }

    public ChartFetchErrorType type() {
        return type;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    private static String buildMessage(ChartFetchErrorType type, Duration retryAfter) {
        String retryPart = retryAfter == null ? "<none>" : retryAfter.toString();
        return "ChartFetchException{type=" + type + ", retryAfter=" + retryPart + "}";
    }
}
