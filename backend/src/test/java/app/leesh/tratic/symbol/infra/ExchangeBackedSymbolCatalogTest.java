package app.leesh.tratic.symbol.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import app.leesh.tratic.shared.market.Market;
import app.leesh.tratic.symbol.domain.CatalogSymbol;
import app.leesh.tratic.symbol.service.SymbolSearchQuery;

class ExchangeBackedSymbolCatalogTest {

    @Test
    void after_properties_set_loads_catalog_from_sources() throws Exception {
        ExchangeBackedSymbolCatalog catalog = new ExchangeBackedSymbolCatalog(List.of(
                source(Market.UPBIT, List.of(
                        new CatalogSymbol(Market.UPBIT, "KRW-BTC", "비트코인 / Bitcoin"),
                        new CatalogSymbol(Market.UPBIT, "KRW-ETH", "이더리움 / Ethereum"))),
                source(Market.BINANCE, List.of(
                        new CatalogSymbol(Market.BINANCE, "BTCUSDT", "BTC / USDT")))));

        catalog.afterPropertiesSet();

        assertTrue(catalog.contains(Market.UPBIT, "KRW-BTC"));
        assertTrue(catalog.contains(Market.BINANCE, "BTCUSDT"));
        assertFalse(catalog.contains(Market.BINANCE, "ETHUSDT"));
    }

    @Test
    void search_filters_by_market_and_keyword() throws Exception {
        ExchangeBackedSymbolCatalog catalog = new ExchangeBackedSymbolCatalog(List.of(
                source(Market.UPBIT, List.of(
                        new CatalogSymbol(Market.UPBIT, "KRW-BTC", "비트코인 / Bitcoin"),
                        new CatalogSymbol(Market.UPBIT, "KRW-XRP", "리플 / Ripple")))));

        catalog.afterPropertiesSet();

        List<CatalogSymbol> result = catalog.search(new SymbolSearchQuery(Market.UPBIT, "비트", 10));

        assertEquals(1, result.size());
        assertEquals("KRW-BTC", result.get(0).nativeSymbolCode());
    }

    @Test
    void after_properties_set_fails_when_source_is_empty() {
        ExchangeBackedSymbolCatalog catalog = new ExchangeBackedSymbolCatalog(List.of());

        assertThrows(IllegalStateException.class, catalog::afterPropertiesSet);
    }

    @Test
    void after_properties_set_fails_when_symbol_market_does_not_match_source_market() {
        ExchangeBackedSymbolCatalog catalog = new ExchangeBackedSymbolCatalog(List.of(
                source(Market.UPBIT, List.of(
                        new CatalogSymbol(Market.BINANCE, "BTCUSDT", "BTC / USDT")))));

        assertThrows(IllegalStateException.class, catalog::afterPropertiesSet);
    }

    private SymbolCatalogSource source(Market market, List<CatalogSymbol> symbols) {
        return new SymbolCatalogSource() {
            @Override
            public Market market() {
                return market;
            }

            @Override
            public List<CatalogSymbol> fetchAll() {
                return symbols;
            }
        };
    }
}
