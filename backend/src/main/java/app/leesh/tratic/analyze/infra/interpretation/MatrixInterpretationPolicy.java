package app.leesh.tratic.analyze.infra.interpretation;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import app.leesh.tratic.analyze.domain.interpretation.AnalyzeScenario;

public record MatrixInterpretationPolicy(
        List<MatrixAnalyzeScoreBand> trendBands,
        List<MatrixAnalyzeScoreBand> volatilityBands,
        List<MatrixAnalyzeScoreBand> locationBands,
        List<MatrixAnalyzeScoreBand> pressureBands,
        List<MatrixAnalyzeScenarioRule> prioritizedScenarios,
        MatrixAnalyzeScenarioRule fallbackScenario,
        String policyVersion) {

    public MatrixInterpretationPolicy {
        trendBands = sortedBands(trendBands, "trendBands", -100.0, 100.0);
        volatilityBands = sortedBands(volatilityBands, "volatilityBands", 0.0, 100.0);
        locationBands = sortedBands(locationBands, "locationBands", 0.0, 100.0);
        pressureBands = sortedBands(pressureBands, "pressureBands", -100.0, 100.0);
        prioritizedScenarios = immutableCopy(prioritizedScenarios, "prioritizedScenarios");
        Objects.requireNonNull(fallbackScenario, "fallbackScenario must not be null");
        Objects.requireNonNull(policyVersion, "policyVersion must not be null");
        validateFallbackCoverage(trendBands, volatilityBands, locationBands, pressureBands, fallbackScenario);
    }

    /**
     * 외부 설정이 아닌 자바로 만든 내부 도메인 정책 객체
     * @return 차트의 각 상태축에 대한 라벨링 밴드 및 시나리오 정책
     */
    public static MatrixInterpretationPolicy defaultPolicy() {
        return new MatrixInterpretationPolicy(
                List.of(
                        new MatrixAnalyzeScoreBand("STRONG_BEAR", -100.0, -60.0),
                        new MatrixAnalyzeScoreBand("BEAR", -60.0, -20.0),
                        new MatrixAnalyzeScoreBand("NEUTRAL", -20.0, 20.0),
                        new MatrixAnalyzeScoreBand("BULL", 20.0, 60.0),
                        new MatrixAnalyzeScoreBand("STRONG_BULL", 60.0, 100.000001)),
                List.of(
                        new MatrixAnalyzeScoreBand("CALM", 0.0, 35.0),
                        new MatrixAnalyzeScoreBand("BALANCED", 35.0, 70.0),
                        new MatrixAnalyzeScoreBand("EXPANSION", 70.0, 100.000001)),
                List.of(
                        new MatrixAnalyzeScoreBand("DISCOUNT", 0.0, 20.0),
                        new MatrixAnalyzeScoreBand("LOWER_RANGE", 20.0, 40.0),
                        new MatrixAnalyzeScoreBand("MID_RANGE", 40.0, 60.0),
                        new MatrixAnalyzeScoreBand("UPPER_RANGE", 60.0, 80.0),
                        new MatrixAnalyzeScoreBand("PREMIUM", 80.0, 100.000001)),
                List.of(
                        new MatrixAnalyzeScoreBand("STRONG_SELL", -100.0, -60.0),
                        new MatrixAnalyzeScoreBand("SELL", -60.0, -20.0),
                        new MatrixAnalyzeScoreBand("NEUTRAL", -20.0, 20.0),
                        new MatrixAnalyzeScoreBand("BUY", 20.0, 60.0),
                        new MatrixAnalyzeScoreBand("STRONG_BUY", 60.0, 100.000001)),
                List.of(
                        new MatrixAnalyzeScenarioRule(
                                AnalyzeScenario.BULLISH_BREAKOUT_EXTENSION,
                                "MOMENTUM_UP",
                                "HIGH",
                                "HIGH",
                                List.of("STRONG_BULL"),
                                List.of("EXPANSION"),
                                List.of("UPPER_RANGE", "PREMIUM"),
                                List.of("STRONG_BUY")),
                        new MatrixAnalyzeScenarioRule(
                                AnalyzeScenario.BEARISH_BREAKOUT_EXTENSION,
                                "MOMENTUM_DOWN",
                                "HIGH",
                                "HIGH",
                                List.of("STRONG_BEAR"),
                                List.of("EXPANSION"),
                                List.of("LOWER_RANGE", "DISCOUNT"),
                                List.of("STRONG_SELL")),
                        new MatrixAnalyzeScenarioRule(
                                AnalyzeScenario.BULLISH_TREND_CONTINUATION,
                                "CONTINUATION_UP",
                                "HIGH",
                                "MEDIUM",
                                List.of("BULL", "STRONG_BULL"),
                                List.of("BALANCED", "EXPANSION"),
                                List.of("MID_RANGE", "UPPER_RANGE"),
                                List.of("BUY", "STRONG_BUY")),
                        new MatrixAnalyzeScenarioRule(
                                AnalyzeScenario.BEARISH_TREND_CONTINUATION,
                                "CONTINUATION_DOWN",
                                "HIGH",
                                "MEDIUM",
                                List.of("BEAR", "STRONG_BEAR"),
                                List.of("BALANCED", "EXPANSION"),
                                List.of("MID_RANGE", "LOWER_RANGE"),
                                List.of("SELL", "STRONG_SELL")),
                        new MatrixAnalyzeScenarioRule(
                                AnalyzeScenario.DISTRIBUTION_WARNING,
                                "EXHAUSTION",
                                "MEDIUM",
                                "HIGH",
                                List.of("BULL", "STRONG_BULL"),
                                List.of("BALANCED", "EXPANSION"),
                                List.of("UPPER_RANGE", "PREMIUM"),
                                List.of("SELL", "STRONG_SELL", "NEUTRAL")),
                        new MatrixAnalyzeScenarioRule(
                                AnalyzeScenario.ACCUMULATION_WARNING,
                                "REVERSAL_WATCH",
                                "MEDIUM",
                                "MEDIUM",
                                List.of("BEAR", "STRONG_BEAR"),
                                List.of("BALANCED", "EXPANSION"),
                                List.of("LOWER_RANGE", "DISCOUNT"),
                                List.of("BUY", "STRONG_BUY", "NEUTRAL")),
                        new MatrixAnalyzeScenarioRule(
                                AnalyzeScenario.FAILED_BREAKOUT_RISK,
                                "FAILED_BREAKOUT_WATCH",
                                "MEDIUM",
                                "HIGH",
                                List.of("STRONG_BULL", "STRONG_BEAR", "BULL", "BEAR"),
                                List.of("EXPANSION", "BALANCED"),
                                List.of("UPPER_RANGE", "PREMIUM", "LOWER_RANGE", "DISCOUNT"),
                                List.of("NEUTRAL")),
                        new MatrixAnalyzeScenarioRule(
                                AnalyzeScenario.RANGE_ROTATION,
                                "RANGE",
                                "MEDIUM",
                                "LOW",
                                List.of("NEUTRAL"),
                                List.of("CALM", "BALANCED", "UNRESOLVED"),
                                List.of("LOWER_RANGE", "MID_RANGE", "UPPER_RANGE"),
                                List.of("BUY", "SELL", "NEUTRAL")),
                        new MatrixAnalyzeScenarioRule(
                                AnalyzeScenario.LOW_VOL_COMPRESSION,
                                "COMPRESSION",
                                "LOW",
                                "LOW",
                                List.of("NEUTRAL", "BULL", "BEAR"),
                                List.of("CALM", "UNRESOLVED"),
                                List.of("DISCOUNT", "LOWER_RANGE", "MID_RANGE", "UPPER_RANGE", "PREMIUM"),
                                List.of("BUY", "SELL", "NEUTRAL"))),
                new MatrixAnalyzeScenarioRule(
                        AnalyzeScenario.INDECISIVE_TRANSITION,
                        "TRANSITION",
                        "LOW",
                        "MEDIUM",
                        List.of("STRONG_BEAR", "BEAR", "NEUTRAL", "BULL", "STRONG_BULL"),
                        List.of("CALM", "BALANCED", "EXPANSION"),
                        List.of("DISCOUNT", "LOWER_RANGE", "MID_RANGE", "UPPER_RANGE", "PREMIUM"),
                        List.of("STRONG_BUY", "BUY", "NEUTRAL", "SELL", "STRONG_SELL")),
                "matrix-v1");
    }

    private static List<MatrixAnalyzeScoreBand> sortedBands(List<MatrixAnalyzeScoreBand> source, String fieldName,
            double expectedMinInclusive, double expectedMaxInclusive) {
        List<MatrixAnalyzeScoreBand> sorted = immutableCopy(source, fieldName).stream()
                .sorted(Comparator.comparingDouble(MatrixAnalyzeScoreBand::minInclusive))
                .toList();

        MatrixAnalyzeScoreBand first = sorted.get(0);
        if (first.minInclusive() > expectedMinInclusive) {
            throw new IllegalArgumentException(fieldName + " bands must start at or before " + expectedMinInclusive);
        }

        for (int i = 1; i < sorted.size(); i++) {
            MatrixAnalyzeScoreBand previous = sorted.get(i - 1);
            MatrixAnalyzeScoreBand current = sorted.get(i);
            if (previous.maxExclusive() > current.minInclusive()) {
                throw new IllegalArgumentException(fieldName + " bands must not overlap");
            }
            if (previous.maxExclusive() < current.minInclusive()) {
                throw new IllegalArgumentException(fieldName + " bands must not have gaps");
            }
        }

        MatrixAnalyzeScoreBand last = sorted.get(sorted.size() - 1);
        if (last.maxExclusive() <= expectedMaxInclusive) {
            throw new IllegalArgumentException(fieldName + " bands must end after " + expectedMaxInclusive);
        }

        return sorted;
    }

    private static <T> List<T> immutableCopy(List<T> source, String fieldName) {
        Objects.requireNonNull(source, fieldName + " must not be null");
        if (source.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return List.copyOf(source);
    }

    private static void validateFallbackCoverage(
            List<MatrixAnalyzeScoreBand> trendBands,
            List<MatrixAnalyzeScoreBand> volatilityBands,
            List<MatrixAnalyzeScoreBand> locationBands,
            List<MatrixAnalyzeScoreBand> pressureBands,
            MatrixAnalyzeScenarioRule fallbackScenario) {
        validateAxisCoverage("fallbackScenario.trend", scoreSignalsOf(trendBands), fallbackScenario.trend());
        validateAxisCoverage("fallbackScenario.volatility", scoreSignalsOf(volatilityBands),
                fallbackScenario.volatility());
        validateAxisCoverage("fallbackScenario.location", scoreSignalsOf(locationBands), fallbackScenario.location());
        validateAxisCoverage("fallbackScenario.pressure", scoreSignalsOf(pressureBands), fallbackScenario.pressure());
    }

    private static void validateAxisCoverage(String fieldName, Set<String> expectedSignals, List<String> actualSignals) {
        Set<String> normalized = new LinkedHashSet<>(actualSignals);
        if (!normalized.containsAll(expectedSignals)) {
            throw new IllegalArgumentException(fieldName + " must include all configured signals");
        }
    }

    private static Set<String> scoreSignalsOf(List<MatrixAnalyzeScoreBand> bands) {
        return bands.stream()
                .map(MatrixAnalyzeScoreBand::signal)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

}
