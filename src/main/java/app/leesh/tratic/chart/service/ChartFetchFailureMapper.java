package app.leesh.tratic.chart.service;

import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.infra.shared.MarketException;

final class ChartFetchFailureMapper {

    private ChartFetchFailureMapper() {
    }

    static ChartFetchFailure map(MarketException ex, Market market) {
        return switch (ex.type()) {
            case TEMPORARY -> new ChartFetchFailure.Temporary(market, ex.httpStatus());
            case RATE_LIMITED -> new ChartFetchFailure.RateLimited(market, ex.httpStatus(), ex.retryAfter());
            case INVALID_REQUEST -> new ChartFetchFailure.InvalidRequest(market, ex.httpStatus());
            case UNAUTHORIZED -> new ChartFetchFailure.Unauthorized(market, ex.httpStatus());
            case NOT_FOUND -> new ChartFetchFailure.NotFound(market, ex.httpStatus());
        };
    }
}
