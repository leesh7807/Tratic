package app.leesh.tratic.chart.infra.upbit;

import java.io.IOException;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.leesh.tratic.chart.infra.shared.MarketErrorType;

@Component
public class UpbitApiClient {

        private final RestClient client;
        private final ObjectMapper om;

        public UpbitApiClient(RestClient.Builder builder, ObjectMapper om) {
                this.client = builder.baseUrl("https://api.upbit.com/v1").build();
                this.om = om;
        }

        /**
         * upbit 분봉 캔들 요청
         * 
         * @param unit   원하는 분봉 (1,3,5,10,15,30,60,240)
         * @param market 조회 요청하는 페어 ex) KRW-BTC
         * @param to     조회 기간의 종료 시각
         * @param count  캔들 개수 <= 200
         */
        public UpbitCandleResponse[] fetchMinuteCandles(long unit, String market, String to, long count) {
                return fetchCandles("/candles/minutes/{unit}", market, to, count, unit);
        }

        /**
         * upbit 일봉 캔들 요청
         * 
         * @param market 조회 요청하는 페어 ex) KRW-BTC
         * @param to     조회 기간의 종료 시각
         * @param count  캔들 개수 <= 200
         */
        public UpbitCandleResponse[] fetchDayCandles(String market, String to, long count) {
                return fetchCandles("/candles/days", market, to, count, null);
        }

        private UpbitCandleResponse[] fetchCandles(@NonNull String path, String market, String to, long count,
                        Long unit) {
                return client.get()
                                .uri(uriBuilder -> {
                                        if (unit != null) {
                                                return uriBuilder
                                                                .path(path)
                                                                .queryParam("market", market)
                                                                .queryParam("to", to)
                                                                .queryParam("count", count)
                                                                .build(unit);
                                        }

                                        return uriBuilder
                                                        .path(path)
                                                        .queryParam("market", market)
                                                        .queryParam("to", to)
                                                        .queryParam("count", count)
                                                        .build();
                                })
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, (req, res) -> {
                                        int status = res.getStatusCode().value();
                                        String rawMessage = null;
                                        try {
                                                UpbitErrorEnvelope env = om.readValue(res.getBody(),
                                                                UpbitErrorEnvelope.class);
                                                if (env != null && env.error() != null) {
                                                        rawMessage = env.error().message();
                                                }
                                        } catch (IOException ignore) {
                                                rawMessage = "failed to parse upbit error body";
                                        }
                                        UpbitErrorType upbitType = UpbitErrorType.from(status, rawMessage);
                                        MarketErrorType marketType = upbitType.toMarketErrorType();

                                        // retry-after 계산 없이 상위 정책 폴백에 맡기는 상태
                                        throw marketType.exception(status, rawMessage, null);
                                })
                                .body(UpbitCandleResponse[].class);
        }

        // Upbit 에러 바디 파싱용 DTO
        public record UpbitErrorEnvelope(UpbitError error) {
        }

        public record UpbitError(int name, String message) {
        }
}
