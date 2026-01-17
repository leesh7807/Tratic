package app.leesh.tratic.chart.service;

import java.time.Duration;

import app.leesh.tratic.chart.domain.Market;

public sealed interface ChartFetchFailure permits
        ChartFetchFailure.Temporary,
        ChartFetchFailure.RateLimited,
        ChartFetchFailure.InvalidRequest,
        ChartFetchFailure.Unauthorized,
        ChartFetchFailure.NotFound {

    Market market();

    int httpStatus();

    /**
     * 일시적 장애 (네트워크/타임아웃/5xx 등)
     * - 즉시 재시도 대상
     */
    record Temporary(Market market, int httpStatus) implements ChartFetchFailure {
    }

    /**
     * 레이트 리밋
     * - 즉시 재시도 X
     * - retryAfter가 제공되면 그만큼 기다렸다가 재시도
     * - 제공되지 않으면 보수적으로 백오프(예: 30s, 60s...) 같은 정책을 상위에서 적용
     */
    record RateLimited(Market market, int httpStatus, Duration retryAfter) implements ChartFetchFailure {
    }

    /**
     * 요청 자체가 잘못됨 (파라미터/기간/캔들 개수/타임프레임 등)
     * - 재시도해도 해결되지 않음 (수정 필요)
     */
    record InvalidRequest(Market market, int httpStatus) implements ChartFetchFailure {
    }

    /**
     * 인증/권한 문제 (API 키 만료, 서명 실패, 권한 없음 등)
     * - 재시도보다 설정/시크릿 문제 확인이 먼저
     */
    record Unauthorized(Market market, int httpStatus) implements ChartFetchFailure {
    }

    /**
     * 존재하지 않음 (심볼/마켓/리소스 없음)
     * - 심볼 유효성/상장 여부/마켓 지원 여부 등을 확인
     */
    record NotFound(Market market, int httpStatus) implements ChartFetchFailure {
    }
}