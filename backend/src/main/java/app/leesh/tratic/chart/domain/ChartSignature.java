package app.leesh.tratic.chart.domain;

import java.util.Objects;

import app.leesh.tratic.shared.market.Market;

public record ChartSignature(
        Market market,
        MarketSymbol symbol,
        TimeResolution timeResolution) {

    public ChartSignature {
        Objects.requireNonNull(market, "market must not be null");
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(timeResolution, "time resolution must not be null");
    }
}
