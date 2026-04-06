package app.leesh.tratic.analyze.infra.config;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "analyze")
public record AnalyzeProps(
        @Positive long fetchCandleCount,
        @NotNull @Valid AnalyzeEngineProps engine,
        @NotNull @Valid AnalyzeClassificationProps classification) {

    public AnalyzeProps {
        Objects.requireNonNull(engine, "engine must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
    }
}
