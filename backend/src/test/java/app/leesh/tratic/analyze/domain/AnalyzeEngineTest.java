package app.leesh.tratic.analyze.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.leesh.tratic.analyze.domain.classification.AnalyzeClassificationSpec;
import app.leesh.tratic.analyze.domain.classification.LocationBand;
import app.leesh.tratic.analyze.domain.classification.PressureBand;
import app.leesh.tratic.analyze.domain.classification.ScoreClassificationRange;
import app.leesh.tratic.analyze.domain.classification.ScoreClassificationRanges;
import app.leesh.tratic.analyze.domain.classification.TrendBand;
import app.leesh.tratic.chart.domain.Candle;
import app.leesh.tratic.chart.domain.TimeResolution;

public class AnalyzeEngineTest {
    @Test
    @DisplayName("밴드 범위를 변경하면 trend/location/pressure 스코어 범위도 함께 변경된다")
    public void analyze_scales_scores_to_configured_band_ranges() {
        AnalyzeClassificationSpec classificationSpec = new AnalyzeClassificationSpec(
                new ScoreClassificationRanges<>(List.of(
                        new ScoreClassificationRange<>(TrendBand.STRONG_BEAR, -2.0, -1.0),
                        new ScoreClassificationRange<>(TrendBand.BEAR, -1.0, -0.2),
                        new ScoreClassificationRange<>(TrendBand.NEUTRAL, -0.2, 0.2),
                        new ScoreClassificationRange<>(TrendBand.BULL, 0.2, 1.0),
                        new ScoreClassificationRange<>(TrendBand.STRONG_BULL, 1.0, 2.1))),
                new ScoreClassificationRanges<>(List.of(
                        new ScoreClassificationRange<>(LocationBand.LOWEST, 10.0, 12.0),
                        new ScoreClassificationRange<>(LocationBand.LOWER, 12.0, 14.0),
                        new ScoreClassificationRange<>(LocationBand.MIDDLE, 14.0, 16.0),
                        new ScoreClassificationRange<>(LocationBand.UPPER, 16.0, 18.0),
                        new ScoreClassificationRange<>(LocationBand.HIGHEST, 18.0, 20.1))),
                new ScoreClassificationRanges<>(List.of(
                        new ScoreClassificationRange<>(PressureBand.STRONG_SELL, -5.0, -3.0),
                        new ScoreClassificationRange<>(PressureBand.SELL, -3.0, -1.0),
                        new ScoreClassificationRange<>(PressureBand.NEUTRAL, -1.0, 1.0),
                        new ScoreClassificationRange<>(PressureBand.BUY, 1.0, 3.0),
                        new ScoreClassificationRange<>(PressureBand.STRONG_BUY, 3.0, 5.1))));
        AnalyzeSpecResolver specResolver = resolution -> new AnalyzeSpec(defaultParams(), classificationSpec);
        AnalyzeEngine analyzeEngine = new AnalyzeEngine(specResolver);

        AnalyzeResult result = analyzeEngine.analyze(sampleCandles(260), TimeResolution.M15, AnalyzeDirection.LONG);

        assertTrue(result.trendScore() >= -2.0);
        assertTrue(result.trendScore() < 2.1);
        assertTrue(result.locationScore() >= 10.0);
        assertTrue(result.locationScore() < 20.1);
        assertTrue(result.pressureScore() >= -5.0);
        assertTrue(result.pressureScore() < 5.1);
        var classified = classificationSpec.classify(result);
        assertNotNull(classified.trend());
        assertNotNull(classified.location());
        assertNotNull(classified.pressure());
    }

    private AnalyzeEngineParams defaultParams() {
        return new AnalyzeEngineParams(
                1e-9,
                20,
                10,
                30,
                0.1,
                1e-6,
                20,
                30,
                3.0,
                1.45,
                0.65,
                20,
                14,
                100,
                5,
                20,
                5,
                0.6,
                0.3,
                0.1,
                0.5,
                1.5);
    }

    private List<Candle> sampleCandles(int candleCount) {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < candleCount; i++) {
            BigDecimal open = bd(100 + i * 0.12);
            BigDecimal close = bd(100 + i * 0.14 + (i % 4) * 0.03);
            BigDecimal high = close.max(open).add(bd("0.4"));
            BigDecimal low = close.min(open).subtract(bd("0.2"));
            candles.add(new Candle(
                    start.plusSeconds(i * 15L * 60L),
                    open,
                    high,
                    low,
                    close,
                    bd(1200 + i * 15)));
        }
        return candles;
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }
}
