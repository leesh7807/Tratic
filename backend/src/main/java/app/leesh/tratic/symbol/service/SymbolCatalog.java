package app.leesh.tratic.symbol.service;

import java.util.List;

import app.leesh.tratic.shared.market.Market;
import app.leesh.tratic.symbol.domain.CatalogSymbol;

public interface SymbolCatalog {
    List<CatalogSymbol> search(SymbolSearchQuery query);

    boolean contains(Market market, String nativeSymbolCode);
}
