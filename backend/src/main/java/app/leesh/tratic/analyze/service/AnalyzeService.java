package app.leesh.tratic.analyze.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.AnalyzeEngine;
import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.service.error.AnalyzeFailure;
import app.leesh.tratic.chart.domain.Candle;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Symbol;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.chart.service.ChartFetchRequest;
import app.leesh.tratic.chart.service.ChartService;
import app.leesh.tratic.shared.Result;

@Service
public class AnalyzeService {
    private final ChartService chartService;
    private final AnalyzeFetchCountConfig analyzeFetchCountConfig;
    private final AnalyzeEngine analyzeEngine;
    private final AnalyzeResultRepository analyzeResultRepository;

    public AnalyzeService(ChartService chartService, AnalyzeFetchCountConfig analyzeFetchCountConfig,
            AnalyzeEngine analyzeEngine,
            AnalyzeResultRepository analyzeResultRepository) {
        this.chartService = chartService;
        this.analyzeFetchCountConfig = analyzeFetchCountConfig;
        this.analyzeEngine = analyzeEngine;
        this.analyzeResultRepository = analyzeResultRepository;
    }

    public Result<AnalyzeResult, AnalyzeFailure> analyze(AnalyzeRequest request, UUID authenticatedUserId) {
        return collectCandles(request)
                .flatMap(candles -> analyzeCandles(candles, request.resolution(), request.direction()))
                .map(analyzed -> persist(authenticatedUserId, request, analyzed));
    }

    private Result<List<Candle>, AnalyzeFailure> collectCandles(AnalyzeRequest request) {
        TimeResolution resolution = request.resolution();
        ChartSignature signature = new ChartSignature(request.market(), new Symbol(request.symbol()), resolution);
        Instant asOf = request.entryAt().minus(resolution.toDuration());

        return chartService.collectChart(
                new ChartFetchRequest(signature, asOf, analyzeFetchCountConfig.fetchCandleCount()))
                .mapError(cause -> (AnalyzeFailure) new AnalyzeFailure.ChartDataUnavailable(cause))
                .map(chart -> chart.candlesBeforeBucketOf(request.entryAt()));
    }

    private Result<AnalyzeResult, AnalyzeFailure> analyzeCandles(List<Candle> candles, TimeResolution resolution,
            AnalyzeDirection direction) {
        int minimumCandles = analyzeEngine.minimumRequiredCandles(resolution);
        if (candles.size() < minimumCandles) {
            return Result.err(new AnalyzeFailure.InsufficientCandles(minimumCandles, candles.size()));
        }

        try {
            return Result.ok(analyzeEngine.analyze(candles, resolution, direction));
        } catch (IllegalArgumentException ex) {
            return Result.err(new AnalyzeFailure.InvalidInput(ex.getMessage()));
        }
    }

    private AnalyzeResult persist(UUID authenticatedUserId, AnalyzeRequest request, AnalyzeResult analyzed) {
        if (authenticatedUserId != null) {
            analyzeResultRepository.save(authenticatedUserId, request, analyzed);
        }

        return analyzed;
    }
}
