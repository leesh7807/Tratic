package app.leesh.tratic.analyze.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AnalyzeProps.class)
public class AnalyzeConfig {
}
