package app.leesh.tratic.chart.infra.binance;

import app.leesh.tratic.chart.infra.shared.MarketErrorType;

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
    public MarketErrorType toMarketErrorType() {
        return switch (this) {
            case TEMPORARY -> MarketErrorType.TEMPORARY;
            case RATE_LIMITED -> MarketErrorType.RATE_LIMITED;
            case INVALID_REQUEST -> MarketErrorType.INVALID_REQUEST;
            case UNAUTHORIZED -> MarketErrorType.UNAUTHORIZED;
            case NOT_FOUND -> MarketErrorType.NOT_FOUND;
        };
    }
}
