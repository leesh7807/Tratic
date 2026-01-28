package app.leesh.tratic.chart.infra.binance;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.infra.shared.ClientPropsConfig.BinanceProps;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
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

    public Result<BinanceCandleResponse[], ChartFetchFailure> fetchCandlesTo(ChartSignature sig, String symbol,
            String interval, long to, int limit) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("endTime", to)
                        .queryParam("limit", limit)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange((req, res) -> {
                    int status = res.getStatusCode().value();
                    if (!res.getStatusCode().isError()) {
                        BinanceCandleResponse[] body = om.readValue(res.getBody(), BinanceCandleResponse[].class);
                        return Result.ok(body);
                    }
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
                    ChartFetchFailure failure = binanceType.toFailure(Market.BINANCE, null);
                    log.debug("OUT ERR [binance] status={} uri={} rawMessage={}", status, req.getURI(), rawMessage);
                    // retry-after 계산 없이 상위 정책 폴백에 맡기는 상태
                    return Result.err(failure);
                });
    }

    public record BinanceErrorEnvelope(int code, String msg) {
    }
}
