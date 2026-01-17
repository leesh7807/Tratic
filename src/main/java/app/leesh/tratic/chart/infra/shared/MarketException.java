package app.leesh.tratic.chart.infra.shared;

import java.time.Duration;

public final class MarketException extends RuntimeException {

    private final MarketErrorType type;
    private final int httpStatus;
    private final String rawMessage;
    private final Duration retryAfter;

    public MarketException(MarketErrorType type, int httpStatus, String rawMessage, Duration retryAfter) {
        super(buildMessage(type, httpStatus, rawMessage, retryAfter));
        this.type = type;
        this.httpStatus = httpStatus;
        this.rawMessage = rawMessage;
        this.retryAfter = retryAfter;
    }

    public MarketErrorType type() {
        return type;
    }

    public int httpStatus() {
        return httpStatus;
    }

    /** 마켓이 준 원문 메시지/바디에서 추출한 메시지 그대로 */
    public String rawMessage() {
        return rawMessage;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    private static String buildMessage(MarketErrorType type, int httpStatus, String rawMessage, Duration retryAfter) {
        // 로그/관측에서 유용하게 “타입 + 코드 + 원문”을 한 줄로 남기기 위한 메시지
        String retryPart = retryAfter == null ? "<none>" : retryAfter.toString();
        String msgPart = (rawMessage == null || rawMessage.isBlank()) ? "<empty>" : rawMessage;
        return "MarketException{type=" + type + ", httpStatus=" + httpStatus
                + ", retryAfter=" + retryPart + ", rawMessage=" + msgPart + "}";
    }
}
