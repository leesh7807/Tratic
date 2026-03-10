package app.leesh.tratic.chart.domain;

import java.util.Objects;

public record ChartSignature(
        Market market,
        Symbol symbol,
        TimeResolution timeResolution) {

    public ChartSignature {
        Objects.requireNonNull(market, "market must not be null");
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(timeResolution, "time resolution must not be null");
    }
}