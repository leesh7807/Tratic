package app.leesh.tratic.chart.infra.binance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.Symbol;
import app.leesh.tratic.chart.domain.TimeResolution;

class BinanceApiClientTest {

    @Tag("external")
    @Test
    void fetchCandlesTo_hitsBinanceApi() {
        RestClient.Builder builder = RestClient.builder();
        BinanceApiClient client = new BinanceApiClient(builder, new ObjectMapper());

        ChartSignature sig = new ChartSignature(Market.BINANCE, new Symbol("BTCUSDT"), TimeResolution.M1);
        BinanceCandleResponse[] res = client.fetchCandlesTo(sig, "BTCUSDT", "1m", 1700000000000L, 2);

        assertTrue(res.length > 0);
    }
}
