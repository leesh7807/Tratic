package app.leesh.tratic.chart.service.error;

import app.leesh.tratic.chart.domain.Market;

public final class ChartFetchFailureMapper {

    private ChartFetchFailureMapper() {
    }

    public static ChartFetchFailure map(ChartFetchException ex, Market market) {
        return switch (ex.type()) {
            case TEMPORARY -> new ChartFetchFailure.Temporary(market);
            case RATE_LIMITED -> new ChartFetchFailure.RateLimited(market, ex.retryAfter());
            case INVALID_REQUEST -> new ChartFetchFailure.InvalidRequest(market);
            case UNAUTHORIZED -> new ChartFetchFailure.Unauthorized(market);
            case NOT_FOUND -> new ChartFetchFailure.NotFound(market);
        };
    }
}
