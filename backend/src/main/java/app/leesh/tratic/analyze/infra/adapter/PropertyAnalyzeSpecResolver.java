package app.leesh.tratic.analyze.infra.adapter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import app.leesh.tratic.analyze.domain.AnalyzeEngineParams;
import app.leesh.tratic.analyze.domain.AnalyzeSpec;
import app.leesh.tratic.analyze.domain.AnalyzeSpecResolver;
import app.leesh.tratic.analyze.domain.classification.AnalyzeClassificationSpec;
import app.leesh.tratic.analyze.domain.classification.LocationBand;
import app.leesh.tratic.analyze.domain.classification.PressureBand;
import app.leesh.tratic.analyze.domain.classification.ScoreClassificationRange;
import app.leesh.tratic.analyze.domain.classification.ScoreClassificationRanges;
import app.leesh.tratic.analyze.domain.classification.TrendBand;
import app.leesh.tratic.analyze.infra.config.AnalyzeClassificationProps;
import app.leesh.tratic.analyze.infra.config.AnalyzeEngineProps;
import app.leesh.tratic.analyze.infra.config.AnalyzeProps;
import app.leesh.tratic.analyze.infra.config.ScoreClassificationRangeProps;
import app.leesh.tratic.analyze.infra.config.ScoreClassificationRangesProps;
import app.leesh.tratic.chart.domain.TimeResolution;

@Component
public class PropertyAnalyzeSpecResolver implements AnalyzeSpecResolver {
    private final AnalyzeSpec defaultSpec;
    private final Map<TimeResolution, AnalyzeSpec> specsByResolution;

    public PropertyAnalyzeSpecResolver(AnalyzeProps analyzeProps) {
        validateFetchCount(analyzeProps);
        AnalyzeClassificationSpec classificationSpec = toClassificationSpec(analyzeProps.classification());
        this.defaultSpec = new AnalyzeSpec(analyzeProps.engine().defaults(), classificationSpec);
        this.specsByResolution = analyzeProps.engine().byResolution().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new AnalyzeSpec(entry.getValue(), classificationSpec)));
    }

    @Override
    public AnalyzeSpec resolve(TimeResolution resolution) {
        return specsByResolution.getOrDefault(resolution, defaultSpec);
    }

    private void validateFetchCount(AnalyzeProps props) {
        validateFetchCount(props.fetchCandleCount(), "defaults", props.engine().defaults().minimumRequiredCandles());
        for (Map.Entry<TimeResolution, AnalyzeEngineParams> entry : props.engine().byResolution().entrySet()) {
            validateFetchCount(
                    props.fetchCandleCount(),
                    entry.getKey().name(),
                    entry.getValue().minimumRequiredCandles());
        }
    }

    private void validateFetchCount(long fetchCandleCount, String scope, int minimumRequiredCandles) {
        if (fetchCandleCount < minimumRequiredCandles) {
            throw new IllegalArgumentException(
                    "analyze.fetch-candle-count must be >= " + minimumRequiredCandles + " for " + scope);
        }
    }

    private AnalyzeClassificationSpec toClassificationSpec(AnalyzeClassificationProps classificationProps) {
        return new AnalyzeClassificationSpec(
                toClassificationRanges(classificationProps.trend(), TrendBand::valueOf),
                toClassificationRanges(classificationProps.location(), LocationBand::valueOf),
                toClassificationRanges(classificationProps.pressure(), PressureBand::valueOf));
    }

    private <E extends Enum<E>> ScoreClassificationRanges<E> toClassificationRanges(ScoreClassificationRangesProps props,
            Function<String, E> enumResolver) {
        List<ScoreClassificationRange<E>> ranges = props.ranges().stream()
                .map(range -> toClassificationRange(range, enumResolver))
                .toList();
        return new ScoreClassificationRanges<>(ranges);
    }

    private <E extends Enum<E>> ScoreClassificationRange<E> toClassificationRange(ScoreClassificationRangeProps props,
            Function<String, E> enumResolver) {
        return new ScoreClassificationRange<>(
                enumResolver.apply(props.code()),
                props.minInclusive(),
                props.maxExclusive());
    }
}
