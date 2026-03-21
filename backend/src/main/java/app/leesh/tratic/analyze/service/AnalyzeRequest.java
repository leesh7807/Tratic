package app.leesh.tratic.analyze.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import app.leesh.tratic.shared.market.Market;
import app.leesh.tratic.chart.domain.TimeResolution;

public record AnalyzeRequest(
        Market market,
        String symbol,
        TimeResolution resolution,
        Instant entryAt,
        BigDecimal entryPrice,
        BigDecimal stopLossPrice,
        BigDecimal positionPct) {

    public AnalyzeRequest {
        Objects.requireNonNull(market, "market must not be null");
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(resolution, "resolution must not be null");
        Objects.requireNonNull(entryAt, "entryAt must not be null");
        Objects.requireNonNull(entryPrice, "entryPrice must not be null");
        Objects.requireNonNull(stopLossPrice, "stopLossPrice must not be null");
    }
}
