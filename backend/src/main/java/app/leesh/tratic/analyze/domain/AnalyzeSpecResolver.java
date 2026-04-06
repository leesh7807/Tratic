package app.leesh.tratic.analyze.domain;

import app.leesh.tratic.chart.domain.TimeResolution;

public interface AnalyzeSpecResolver {
    AnalyzeSpec resolve(TimeResolution resolution);
}
