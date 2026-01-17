package app.leesh.tratic.chart.infra.upbit;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpbitCandleResponse(String market,
        String candleDateTimeUtc,
        String candleDateTimeKst,
        BigDecimal openingPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal tradePrice,
        long timestamp,
        BigDecimal candleAccTradePrice,
        BigDecimal candleAccTradeVolume,
        long unit) {

}