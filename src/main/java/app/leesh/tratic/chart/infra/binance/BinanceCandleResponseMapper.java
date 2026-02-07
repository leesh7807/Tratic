package app.leesh.tratic.chart.infra.binance;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import app.leesh.tratic.chart.domain.Candle;
import app.leesh.tratic.chart.domain.CandleSeries;
import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.ChartSignature;

@Component
final class BinanceCandleResponseMapper {

    public Chart toChart(ChartSignature sig, BinanceCandleResponse[] res) {
        List<Candle> candles = toCandles(res);

        return Chart.of(sig, CandleSeries.ofSorted(candles));
    }

    List<Candle> toCandles(BinanceCandleResponse[] res) {
        return Arrays.stream(res)
                .map(r -> new Candle(
                        Instant.ofEpochMilli(r.openTime()),
                        r.open(),
                        r.high(),
                        r.low(),
                        r.close(),
                        r.volume()))
                .sorted(Comparator.comparing(Candle::time))
                .toList();
    }

}
