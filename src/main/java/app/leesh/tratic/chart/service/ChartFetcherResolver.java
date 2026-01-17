package app.leesh.tratic.chart.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import app.leesh.tratic.chart.domain.Market;

@Component
public class ChartFetcherResolver {
    Map<Market, ChartFetcher> map = new HashMap<>();

    public ChartFetcherResolver(List<ChartFetcher> fetchers) {
        for (ChartFetcher fetcher : fetchers) {
            Market market = fetcher.market();

            if (map.containsKey(market)) {
                throw new IllegalArgumentException("There is two fetchers for one market: " + market.name());
            }

            map.put(market, fetcher);
        }
    }

    public ChartFetcher resolve(Market market) {
        if (map.containsKey(market)) {
            return map.get(market);
        } else {
            throw new IllegalArgumentException("Unsupported market type: " + market.name());
        }
    }
}
