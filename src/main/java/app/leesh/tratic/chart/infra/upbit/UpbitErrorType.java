package app.leesh.tratic.chart.infra.upbit;

import app.leesh.tratic.chart.infra.shared.MarketErrorType;

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
