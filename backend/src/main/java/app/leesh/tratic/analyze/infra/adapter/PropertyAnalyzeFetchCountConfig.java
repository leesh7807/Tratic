package app.leesh.tratic.analyze.infra.adapter;

import org.springframework.stereotype.Component;

import app.leesh.tratic.analyze.infra.config.AnalyzeProps;
import app.leesh.tratic.analyze.service.AnalyzeFetchCountConfig;

@Component
public class PropertyAnalyzeFetchCountConfig implements AnalyzeFetchCountConfig {
    private final AnalyzeProps analyzeProps;

    public PropertyAnalyzeFetchCountConfig(AnalyzeProps analyzeProps) {
        this.analyzeProps = analyzeProps;
    }

    @Override
    public long fetchCandleCount() {
        return analyzeProps.fetchCandleCount();
    }
}
