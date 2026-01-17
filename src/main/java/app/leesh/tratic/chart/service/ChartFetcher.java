package app.leesh.tratic.chart.service;

import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.Market;

public interface ChartFetcher {
    Chart fetch(ChartFetchRequest req);

    Market market();
}
