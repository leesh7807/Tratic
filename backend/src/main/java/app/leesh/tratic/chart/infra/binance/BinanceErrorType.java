package app.leesh.tratic.chart.infra.binance;

import java.time.Duration;

import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;

public enum BinanceErrorType {

    TEMPORARY,
    RATE_LIMITED,
    INVALID_REQUEST,
    UNAUTHORIZED,
    NOT_FOUND;

    /**
     * Binance HTTP 응답을 해석
     */
    public static BinanceErrorType from(int httpStatus, String rawMessage) {
        return switch (httpStatus) {
            case 403, 418 -> UNAUTHORIZED;
            case 404 -> NOT_FOUND;
            case 408 -> TEMPORARY;
            case 429 -> RATE_LIMITED;
            default -> {
                if (httpStatus >= 400 && httpStatus <= 499) {
                    yield INVALID_REQUEST;
                }
                yield fallback(httpStatus, rawMessage);
            }
        };
    }

    private static BinanceErrorType fallback(int httpStatus, String rawMessage) {
        return TEMPORARY;
    }

    /**
     * Binance -> Market 공통 에러로 변환
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
