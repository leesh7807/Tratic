package app.leesh.tratic.symbol.infra.binance;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.BinanceProps;
import app.leesh.tratic.shared.market.Market;
import app.leesh.tratic.symbol.domain.CatalogSymbol;
import app.leesh.tratic.symbol.infra.SymbolCatalogSource;

@Component
public class BinanceFuturesSymbolCatalogSource implements SymbolCatalogSource {
    private final RestClient client;

    public BinanceFuturesSymbolCatalogSource(RestClient.Builder builder, BinanceProps props) {
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("X-Client-Name", "binance-symbol-catalog")
                .build();
    }

    @Override
    public Market market() {
        return Market.BINANCE;
    }

    @Override
    public List<CatalogSymbol> fetchAll() {
        BinanceExchangeInfoResponse response = client.get()
                .uri("/fapi/v1/exchangeInfo")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(BinanceExchangeInfoResponse.class);

        if (response == null || response.symbols() == null) {
            throw new IllegalStateException("binance symbol catalog response must not be null");
        }

        return response.symbols().stream()
                .map(entry -> new CatalogSymbol(Market.BINANCE, entry.symbol(), displayName(entry.baseAsset(), entry.quoteAsset(), entry.symbol())))
                .toList();
    }

    private static String displayName(String baseAsset, String quoteAsset, String fallback) {
        if (baseAsset != null && !baseAsset.isBlank() && quoteAsset != null && !quoteAsset.isBlank()) {
            return baseAsset + " / " + quoteAsset;
        }
        return fallback;
    }

    public record BinanceExchangeInfoResponse(List<BinanceSymbolResponse> symbols) {
    }

    public record BinanceSymbolResponse(
            String symbol,
            String baseAsset,
            String quoteAsset) {
    }
}
