package app.leesh.tratic.chart.infra.binance;

import java.io.IOException;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.BinanceProps;
import app.leesh.tratic.chart.infra.shared.MarketErrorType;

@Component
public class BinanceApiClient {

    private final RestClient client;
    private final ObjectMapper om;

    public BinanceApiClient(RestClient.Builder builder, BinanceProps props, ObjectMapper om) {
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("X-Client-Name", "binance")
                .build();
        this.om = om;
    }

    public BinanceCandleResponse[] fetchCandlesTo(ChartSignature sig, String symbol, String interval, long to,
            int limit) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("endTime", to)
                        .queryParam("limit", limit)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    int status = res.getStatusCode().value();
                    String rawMessage = null;
                    try {
                        BinanceErrorEnvelope env = om.readValue(res.getBody(), BinanceErrorEnvelope.class);
                        if (env != null && env.msg() != null) {
                            rawMessage = env.msg();
                        }
                    } catch (IOException ignore) {
                        rawMessage = "failed to parse binance error body";
                    }
                    BinanceErrorType binanceType = BinanceErrorType.from(status, rawMessage);
                    MarketErrorType marketType = binanceType.toMarketErrorType();
                    throw marketType.exception(status, rawMessage, null);
                })
                .body(BinanceCandleResponse[].class);
    }

    public record BinanceErrorEnvelope(int code, String msg) {
    }
}
