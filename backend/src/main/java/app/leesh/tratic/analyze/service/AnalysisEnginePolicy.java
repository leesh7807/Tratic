package app.leesh.tratic.analyze.service;

import app.leesh.tratic.analyze.domain.AnalysisEngineParams;
import app.leesh.tratic.chart.domain.TimeResolution;

public interface AnalysisEnginePolicy {
    AnalysisEngineParams resolve(TimeResolution resolution);
}
