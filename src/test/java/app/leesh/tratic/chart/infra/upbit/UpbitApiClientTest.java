package app.leesh.tratic.chart.infra.upbit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@SpringBootTest
class UpbitApiClientTest {

    @Autowired
    UpbitApiClient client;

    @Tag("external")
    @Test
    void fetchMinuteCandles_hitsUpbitApi() {
        Result<UpbitCandleResponse[], ChartFetchFailure> res = client.fetchMinuteCandles(1, "KRW-BTC",
                "2024-01-01T00:00:00Z", 2);
        assertTrue(res instanceof Result.Ok);
        UpbitCandleResponse[] body = ((Result.Ok<UpbitCandleResponse[], ChartFetchFailure>) res).value();
        assertTrue(body.length > 0);
    }

    @Tag("external")
    @Test
    void fetchDayCandles_hitsUpbitApi() {
        Result<UpbitCandleResponse[], ChartFetchFailure> res = client.fetchDayCandles("KRW-BTC",
                "2024-01-01T00:00:00Z", 2);
        assertTrue(res instanceof Result.Ok);
        UpbitCandleResponse[] body = ((Result.Ok<UpbitCandleResponse[], ChartFetchFailure>) res).value();
        assertTrue(body.length > 0);
    }
}
