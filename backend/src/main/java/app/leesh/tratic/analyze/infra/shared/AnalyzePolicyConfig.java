package app.leesh.tratic.analyze.infra.shared;

import java.util.Map;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import app.leesh.tratic.analyze.domain.AnalysisEngineParams;
import app.leesh.tratic.analyze.service.AnalysisEnginePolicy;
import app.leesh.tratic.analyze.service.AnalyzePolicy;
import app.leesh.tratic.chart.domain.TimeResolution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Configuration
@EnableConfigurationProperties(AnalyzePolicyConfig.AnalyzeProps.class)
public class AnalyzePolicyConfig {
    @Validated
    @ConfigurationProperties(prefix = "analyze")
    public record AnalyzeProps(
            @Positive long fetchCandleCount,
            @NotNull @Valid EngineProps engine)
            implements AnalyzePolicy, AnalysisEnginePolicy {

        public AnalyzeProps {
            Objects.requireNonNull(engine, "engine must not be null");

            int defaultRequired = engine.defaults().minimumRequiredCandles();
            if (fetchCandleCount < defaultRequired) {
                throw new IllegalArgumentException(
                        "analyze.fetch-candle-count must be >= " + defaultRequired + " for defaults");
            }

            for (Map.Entry<TimeResolution, AnalysisEngineParams> entry : engine.byResolution().entrySet()) {
                int required = entry.getValue().minimumRequiredCandles();
                if (fetchCandleCount < required) {
                    throw new IllegalArgumentException(
                            "analyze.fetch-candle-count must be >= " + required + " for " + entry.getKey());
                }
            }
        }

        @Override
        public AnalysisEngineParams resolve(TimeResolution resolution) {
            return engine.resolve(resolution);
        }
    }

    public record EngineProps(
            @NotNull @Valid AnalysisEngineParams defaults,
            Map<TimeResolution, @Valid AnalysisEngineParams> byResolution) {

        public EngineProps {
            Objects.requireNonNull(defaults, "defaults must not be null");
            byResolution = byResolution == null ? Map.of() : Map.copyOf(byResolution);
        }

        public AnalysisEngineParams resolve(TimeResolution resolution) {
            return byResolution.getOrDefault(resolution, defaults);
        }
    }
}
