package app.leesh.tratic.symbol.domain;

import java.util.Objects;

import app.leesh.tratic.shared.market.Market;

public record CatalogSymbol(
        Market market,
        String nativeSymbolCode,
        String displayName) {

    public CatalogSymbol {
        Objects.requireNonNull(market, "market must not be null");
        Objects.requireNonNull(nativeSymbolCode, "nativeSymbolCode must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
    }
}
