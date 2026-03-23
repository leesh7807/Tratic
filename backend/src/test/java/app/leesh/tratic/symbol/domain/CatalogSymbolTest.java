package app.leesh.tratic.symbol.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import app.leesh.tratic.shared.market.Market;

class CatalogSymbolTest {

    @Test
    void constructor_requires_market() {
        assertThrows(NullPointerException.class, () -> new CatalogSymbol(null, "BTCUSDT", "비트코인 / 테더"));
    }

    @Test
    void constructor_requires_native_symbol_code() {
        assertThrows(NullPointerException.class, () -> new CatalogSymbol(Market.BINANCE, null, "비트코인 / 테더"));
    }

    @Test
    void constructor_requires_display_name() {
        assertThrows(NullPointerException.class, () -> new CatalogSymbol(Market.BINANCE, "BTCUSDT", null));
    }
}
