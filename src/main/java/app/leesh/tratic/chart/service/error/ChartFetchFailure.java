package app.leesh.tratic.chart.service.error;

import java.time.Duration;

import app.leesh.tratic.chart.domain.Market;

public sealed interface ChartFetchFailure permits
        ChartFetchFailure.Temporary,
        ChartFetchFailure.RateLimited,
        ChartFetchFailure.InvalidRequest,
        ChartFetchFailure.Unauthorized,
        ChartFetchFailure.NotFound {

    Market market();

    /**
     * 일시적 실패 (네트워크/외부 5xx 등)
     * - 즉시 재시도 고려
     */
    record Temporary(Market market) implements ChartFetchFailure {
    }

    /**
     * 레이트 리밋
     * - 즉시 재시도 X
     * - retryAfter가 있으면 해당 시간 대기 후 재시도
     * - 없으면 백오프 정책 적용
     */
    record RateLimited(Market market, Duration retryAfter) implements ChartFetchFailure {
    }

    /**
     * 요청 자체가 잘못됨 (파라미터/기간/캔들 수/타임프레임 등)
     * - 재시도해도 해결되지 않음 (수정 필요)
     */
    record InvalidRequest(Market market) implements ChartFetchFailure {
    }

    /**
     * 인증/권한 문제 (API 키 만료, 서명 실패, 권한 없음 등)
     * - 설정/체크 문제 확인 필요
     */
    record Unauthorized(Market market) implements ChartFetchFailure {
    }

    /**
     * 존재하지 않음 (심볼/마켓/리소스 없음)
     * - 심볼/마켓/리소스 유효성 확인
     */
    record NotFound(Market market) implements ChartFetchFailure {
    }
}
