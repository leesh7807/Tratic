package app.leesh.tratic.chart.infra.upbit;

import java.time.Duration;

import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;

public enum UpbitErrorType {

    TEMPORARY,
    RATE_LIMITED,
    INVALID_REQUEST,
    UNAUTHORIZED,
    NOT_FOUND;

    /**
     * Upbit HTTP 응답을 해석
     */
    public static UpbitErrorType from(int httpStatus, String rawMessage) {
        return switch (httpStatus) {
            case 401 -> UNAUTHORIZED;
            case 404 -> NOT_FOUND;

            case 418, 429 -> RATE_LIMITED;

            case 400 -> INVALID_REQUEST;

            case 500 -> TEMPORARY;

            default -> fallback(httpStatus, rawMessage);
        };
    }

    private static UpbitErrorType fallback(int httpStatus, String rawMessage) {
        // Upbit 명세에 없는 케이스는 보수적으로 TEMPORARY
        return TEMPORARY;
    }

    /**
     * Upbit -> Market 공통 에러로 변환
     */
    public ChartFetchFailure toFailure(Market market, Duration retryAfter) {
        return switch (this) {
            case TEMPORARY -> new ChartFetchFailure.Temporary(market);
            case RATE_LIMITED -> new ChartFetchFailure.RateLimited(market, retryAfter);
            case INVALID_REQUEST -> new ChartFetchFailure.InvalidRequest(market);
            case UNAUTHORIZED -> new ChartFetchFailure.Unauthorized(market);
            case NOT_FOUND -> new ChartFetchFailure.NotFound(market);
        };
    }
}
