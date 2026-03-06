package app.leesh.tratic.analyze.infra.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import app.leesh.tratic.analyze.service.AnalyzePolicy;
import jakarta.validation.constraints.Positive;

@Configuration
@EnableConfigurationProperties(AnalyzePolicyConfig.AnalyzeProps.class)
public class AnalyzePolicyConfig {
    @Bean
    public AnalyzePolicy analyzePolicy(AnalyzeProps props) {
        return props;
    }

    @Validated
    @ConfigurationProperties(prefix = "analyze")
    public record AnalyzeProps(@Positive long fetchCandleCount) implements AnalyzePolicy {
    }
}
