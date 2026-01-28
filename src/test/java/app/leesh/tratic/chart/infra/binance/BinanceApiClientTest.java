package app.leesh.tratic.chart.infra.binance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.Symbol;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@SpringBootTest
class BinanceApiClientTest {

    @Autowired
    BinanceApiClient client;

    @Tag("external")
    @Test
    void fetchCandlesTo_hitsBinanceApi() {

        ChartSignature sig = new ChartSignature(Market.BINANCE, new Symbol("BTCUSDT"), TimeResolution.M1);
        Result<BinanceCandleResponse[], ChartFetchFailure> res = client.fetchCandlesTo(sig, "BTCUSDT", "1m",
                1700000000000L, 2);
        assertTrue(res instanceof Result.Ok);
        BinanceCandleResponse[] body = ((Result.Ok<BinanceCandleResponse[], ChartFetchFailure>) res).value();
        assertTrue(body.length > 0);
    }
}
