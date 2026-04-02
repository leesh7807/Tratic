package app.leesh.tratic.symbol.infra;

import java.util.List;

import app.leesh.tratic.shared.market.Market;
import app.leesh.tratic.symbol.domain.CatalogSymbol;

public interface SymbolCatalogSource {
    Market market();

    List<CatalogSymbol> fetchAll();
}
