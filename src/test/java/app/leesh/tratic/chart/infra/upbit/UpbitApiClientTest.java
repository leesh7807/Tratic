package app.leesh.tratic.chart.infra.upbit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class UpbitApiClientTest {

    @Tag("external")
    @Test
    void fetchMinuteCandles_hitsUpbitApi() {
        RestClient.Builder builder = RestClient.builder();
        UpbitApiClient client = new UpbitApiClient(builder, new ObjectMapper());

        UpbitCandleResponse[] res = client.fetchMinuteCandles(1, "KRW-BTC", "2024-01-01T00:00:00Z", 2);

        assertTrue(res.length > 0);
    }

    @Tag("external")
    @Test
    void fetchDayCandles_hitsUpbitApi() {
        RestClient.Builder builder = RestClient.builder();
        UpbitApiClient client = new UpbitApiClient(builder, new ObjectMapper());

        UpbitCandleResponse[] res = client.fetchDayCandles("KRW-BTC", "2024-01-01T00:00:00Z", 2);

        assertTrue(res.length > 0);
    }
}
