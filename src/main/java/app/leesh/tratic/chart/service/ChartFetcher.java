package app.leesh.tratic.chart.service;

import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

public interface ChartFetcher {
    Result<Chart, ChartFetchFailure> fetch(ChartFetchRequest req);

    Market market();
}
