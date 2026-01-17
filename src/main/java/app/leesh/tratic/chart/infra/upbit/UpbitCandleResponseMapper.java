package app.leesh.tratic.chart.infra.upbit;

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
final class UpbitCandleResponseMapper {
    Chart toChart(ChartSignature sig, UpbitCandleResponse[] res) {
        List<Candle> candles = Arrays.stream(res)
                .map(r -> new Candle(
                        parseUtcInstant(r.candleDateTimeUtc()),
                        r.openingPrice(),
                        r.highPrice(),
                        r.lowPrice(),
                        r.tradePrice(),
                        r.candleAccTradeVolume()))
                .sorted(Comparator.comparing(Candle::time))
                .toList();

        return Chart.of(sig, CandleSeries.ofSorted(candles));
    }

    private Instant parseUtcInstant(String utc) {
        String normalized = utc.endsWith("Z") ? utc : utc + "Z";
        return Instant.parse(normalized);
    }
}
