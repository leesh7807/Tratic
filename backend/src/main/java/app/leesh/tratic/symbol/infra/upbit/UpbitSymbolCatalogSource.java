package app.leesh.tratic.symbol.infra.upbit;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.UpbitProps;
import app.leesh.tratic.shared.market.Market;
import app.leesh.tratic.symbol.domain.CatalogSymbol;
import app.leesh.tratic.symbol.infra.SymbolCatalogSource;

@Component
public class UpbitSymbolCatalogSource implements SymbolCatalogSource {
    private final RestClient client;

    public UpbitSymbolCatalogSource(RestClient.Builder builder, UpbitProps props) {
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("X-Client-Name", "upbit-symbol-catalog")
                .build();
    }

    @Override
    public Market market() {
        return Market.UPBIT;
    }

    @Override
    public List<CatalogSymbol> fetchAll() {
        UpbitMarketResponse[] response = client.get()
                .uri("/market/all")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(UpbitMarketResponse[].class);

        if (response == null) {
            throw new IllegalStateException("upbit symbol catalog response must not be null");
        }

        return List.of(response).stream()
                .map(entry -> new CatalogSymbol(Market.UPBIT, entry.market(), displayName(entry.koreanName(), entry.englishName(), entry.market())))
                .toList();
    }

    private static String displayName(String koreanName, String englishName, String fallback) {
        if (koreanName != null && !koreanName.isBlank() && englishName != null && !englishName.isBlank()) {
            return koreanName + " / " + englishName;
        }
        if (koreanName != null && !koreanName.isBlank()) {
            return koreanName;
        }
        if (englishName != null && !englishName.isBlank()) {
            return englishName;
        }
        return fallback;
    }

    public record UpbitMarketResponse(
            String market,
            @JsonProperty("korean_name")
            String koreanName,
            @JsonProperty("english_name")
            String englishName) {
    }
}
