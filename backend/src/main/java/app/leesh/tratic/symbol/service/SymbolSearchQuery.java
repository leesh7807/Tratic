package app.leesh.tratic.symbol.service;

import java.util.Objects;

import app.leesh.tratic.shared.market.Market;

public record SymbolSearchQuery(
        Market market,
        String keyword,
        int limit) {

    public SymbolSearchQuery {
        Objects.requireNonNull(market, "market must not be null");
        Objects.requireNonNull(keyword, "keyword must not be null");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }
}
