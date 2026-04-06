package app.leesh.tratic.analyze.infra.adapter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import app.leesh.tratic.analyze.domain.AnalyzeEngineParams;
import app.leesh.tratic.analyze.domain.AnalyzeSpec;
import app.leesh.tratic.analyze.domain.AnalyzeSpecResolver;
import app.leesh.tratic.analyze.domain.band.AnalyzeBandRange;
import app.leesh.tratic.analyze.domain.band.AnalyzeBandSet;
import app.leesh.tratic.analyze.domain.band.AnalyzeBandSpec;
import app.leesh.tratic.analyze.domain.band.LocationBand;
import app.leesh.tratic.analyze.domain.band.PressureBand;
import app.leesh.tratic.analyze.domain.band.TrendBand;
import app.leesh.tratic.analyze.infra.config.AnalyzeBandProps;
import app.leesh.tratic.analyze.infra.config.AnalyzeBandRangeProps;
import app.leesh.tratic.analyze.infra.config.AnalyzeBandSetProps;
import app.leesh.tratic.analyze.infra.config.AnalyzeEngineProps;
import app.leesh.tratic.analyze.infra.config.AnalyzeProps;
import app.leesh.tratic.chart.domain.TimeResolution;

@Component
public class PropertyAnalyzeSpecResolver implements AnalyzeSpecResolver {
    private final AnalyzeProps analyzeProps;

    public PropertyAnalyzeSpecResolver(AnalyzeProps analyzeProps) {
        this.analyzeProps = analyzeProps;
        validateFetchCount(analyzeProps);
    }

    @Override
    public AnalyzeSpec resolve(TimeResolution resolution) {
        return new AnalyzeSpec(
                resolveEngineParams(analyzeProps.engine(), resolution),
                toBandSpec(analyzeProps.bands()));
    }

    private AnalyzeEngineParams resolveEngineParams(AnalyzeEngineProps engineProps, TimeResolution resolution) {
        return engineProps.byResolution().getOrDefault(resolution, engineProps.defaults());
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

    private AnalyzeBandSpec toBandSpec(AnalyzeBandProps bandProps) {
        return new AnalyzeBandSpec(
                toBandSet(bandProps.trend(), TrendBand::valueOf),
                toBandSet(bandProps.location(), LocationBand::valueOf),
                toBandSet(bandProps.pressure(), PressureBand::valueOf));
    }

    private <E extends Enum<E>> AnalyzeBandSet<E> toBandSet(AnalyzeBandSetProps props, Function<String, E> enumResolver) {
        List<AnalyzeBandRange<E>> ranges = props.ranges().stream()
                .map(range -> toBandRange(range, enumResolver))
                .toList();
        return new AnalyzeBandSet<>(ranges);
    }

    private <E extends Enum<E>> AnalyzeBandRange<E> toBandRange(AnalyzeBandRangeProps props,
            Function<String, E> enumResolver) {
        return new AnalyzeBandRange<>(
                enumResolver.apply(props.code()),
                props.minInclusive(),
                props.maxExclusive());
    }
}
