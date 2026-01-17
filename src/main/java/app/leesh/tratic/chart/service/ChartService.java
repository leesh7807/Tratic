package app.leesh.tratic.chart.service;

import org.springframework.stereotype.Service;

import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.infra.shared.MarketException;
import app.leesh.tratic.shared.Result;

@Service
public class ChartService {
    ChartFetcherResolver resolver;

    public ChartService(ChartFetcherResolver resolver) {
        this.resolver = resolver;
    }

    // 분석하는 쪽에서 차트 수집을 요청
    public Result<Chart, ChartFetchFailure> collectChart(ChartFetchRequest req) {
        ChartFetcher fetcher = resolver.resolve(req.sig().market());
        try {
            return Result.ok(fetcher.fetch(req));
        } catch (MarketException e) {
            return Result.err(ChartFetchFailureMapper.map(e, req.sig().market()));
        }
    }
}
