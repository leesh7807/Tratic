package app.leesh.tratic.chart.infra.upbit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UpbitApiClientTest {

    @Autowired
    UpbitApiClient client;

    @Tag("external")
    @Test
    void fetchMinuteCandles_hitsUpbitApi() {
        UpbitCandleResponse[] res = client.fetchMinuteCandles(1, "KRW-BTC", "2024-01-01T00:00:00Z", 2);

        assertTrue(res.length > 0);
    }

    @Tag("external")
    @Test
    void fetchDayCandles_hitsUpbitApi() {
        UpbitCandleResponse[] res = client.fetchDayCandles("KRW-BTC", "2024-01-01T00:00:00Z", 2);

        assertTrue(res.length > 0);
    }
}
