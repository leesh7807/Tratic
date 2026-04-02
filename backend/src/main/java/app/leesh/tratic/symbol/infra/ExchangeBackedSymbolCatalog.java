package app.leesh.tratic.symbol.infra;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import app.leesh.tratic.shared.market.Market;
import app.leesh.tratic.symbol.domain.CatalogSymbol;
import app.leesh.tratic.symbol.service.SymbolCatalog;
import app.leesh.tratic.symbol.service.SymbolSearchQuery;

@Component
public class ExchangeBackedSymbolCatalog implements SymbolCatalog, InitializingBean {
    private final List<SymbolCatalogSource> sources;
    private volatile Map<Market, List<CatalogSymbol>> catalogByMarket = Map.of();

    public ExchangeBackedSymbolCatalog(List<SymbolCatalogSource> sources) {
        this.sources = List.copyOf(sources);
    }

    @Override
    public void afterPropertiesSet() {
        if (sources.isEmpty()) {
            throw new IllegalStateException("symbol catalog sources must not be empty");
        }
        this.catalogByMarket = loadCatalog();
    }

    @Override
    public List<CatalogSymbol> search(SymbolSearchQuery query) {
        String normalizedKeyword = query.keyword().trim().toLowerCase(Locale.ROOT);
        return catalogByMarket.getOrDefault(query.market(), List.of()).stream()
                .filter(symbol -> matches(symbol, normalizedKeyword))
                .limit(query.limit())
                .toList();
    }

    @Override
    public boolean contains(Market market, String nativeSymbolCode) {
        if (nativeSymbolCode == null) {
            return false;
        }

        return catalogByMarket.getOrDefault(market, List.of()).stream()
                .anyMatch(symbol -> symbol.nativeSymbolCode().equals(nativeSymbolCode));
    }

    private Map<Market, List<CatalogSymbol>> loadCatalog() {
        Map<Market, List<CatalogSymbol>> loaded = new EnumMap<>(Market.class);
        for (SymbolCatalogSource source : sources) {
            List<CatalogSymbol> fetched = source.fetchAll();
            loaded.put(source.market(), deduplicate(source.market(), fetched));
        }
        return Map.copyOf(loaded);
    }

    private List<CatalogSymbol> deduplicate(Market market, List<CatalogSymbol> symbols) {
        Map<String, CatalogSymbol> deduplicated = new LinkedHashMap<>();
        for (CatalogSymbol symbol : symbols) {
            if (symbol.market() != market) {
                throw new IllegalStateException("symbol market does not match source market");
            }
            deduplicated.put(symbol.nativeSymbolCode(), symbol);
        }
        List<CatalogSymbol> ordered = new ArrayList<>(deduplicated.values());
        ordered.sort(Comparator.comparing(CatalogSymbol::nativeSymbolCode));
        return List.copyOf(ordered);
    }

    private boolean matches(CatalogSymbol symbol, String normalizedKeyword) {
        if (normalizedKeyword.isBlank()) {
            return true;
        }

        return symbol.nativeSymbolCode().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                || symbol.displayName().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }
}
