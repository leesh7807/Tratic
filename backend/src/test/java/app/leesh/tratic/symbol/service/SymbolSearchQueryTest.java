package app.leesh.tratic.symbol.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import app.leesh.tratic.shared.market.Market;

class SymbolSearchQueryTest {

    @Test
    void constructor_requires_market() {
        assertThrows(NullPointerException.class, () -> new SymbolSearchQuery(null, "btc", 20));
    }

    @Test
    void constructor_requires_keyword() {
        assertThrows(NullPointerException.class, () -> new SymbolSearchQuery(Market.UPBIT, null, 20));
    }

    @Test
    void constructor_requires_positive_limit() {
        assertThrows(IllegalArgumentException.class, () -> new SymbolSearchQuery(Market.UPBIT, "btc", 0));
    }
}
