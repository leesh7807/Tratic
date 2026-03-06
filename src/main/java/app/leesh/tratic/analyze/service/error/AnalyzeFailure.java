package app.leesh.tratic.analyze.service.error;

import app.leesh.tratic.chart.service.error.ChartFetchFailure;

public sealed interface AnalyzeFailure permits AnalyzeFailure.InvalidInput, AnalyzeFailure.ChartDataUnavailable {

    record InvalidInput(String message) implements AnalyzeFailure {
    }

    record ChartDataUnavailable(ChartFetchFailure cause) implements AnalyzeFailure {
    }
}
