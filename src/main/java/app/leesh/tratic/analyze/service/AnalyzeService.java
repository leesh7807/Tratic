package app.leesh.tratic.analyze.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.domain.AnalysisEngine;
import app.leesh.tratic.analyze.service.error.AnalyzeFailure;
import app.leesh.tratic.chart.domain.Candle;
import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.ChartSignature;
import app.leesh.tratic.chart.domain.Symbol;
import app.leesh.tratic.chart.domain.TimeResolution;
import app.leesh.tratic.chart.service.ChartFetchRequest;
import app.leesh.tratic.chart.service.ChartService;
import app.leesh.tratic.chart.service.error.ChartFetchFailure;
import app.leesh.tratic.shared.Result;

@Service
public class AnalyzeService {
    private final ChartService chartService;
    private final AnalyzePolicy analyzePolicy;
    private final AnalysisResultRepository analysisResultRepository;

    public AnalyzeService(ChartService chartService, AnalyzePolicy analyzePolicy,
            AnalysisResultRepository analysisResultRepository) {
        this.chartService = chartService;
        this.analyzePolicy = analyzePolicy;
        this.analysisResultRepository = analysisResultRepository;
    }

    public Result<AnalyzeResult, AnalyzeFailure> analyze(AnalyzeRequest request, UUID authenticatedUserId) {
        Result<AnalyzeDirection, AnalyzeFailure> directionResult = resolveDirection(request.entryPrice(),
                request.stopLossPrice(), request.takeProfitPrice());

        if (directionResult instanceof Result.Err<AnalyzeDirection, AnalyzeFailure> err) {
            return Result.err(err.error());
        }

        AnalyzeDirection direction = ((Result.Ok<AnalyzeDirection, AnalyzeFailure>) directionResult).value();
        TimeResolution resolution = request.resolution();

        ChartSignature signature = new ChartSignature(request.market(), new Symbol(request.symbol()), resolution);
        Instant asOf = request.entryAt().minus(resolution.toDuration());
        Result<Chart, ChartFetchFailure> chartResult = chartService.collectChart(new ChartFetchRequest(signature, asOf,
                analyzePolicy.fetchCandleCount()));

        if (chartResult instanceof Result.Err<Chart, ChartFetchFailure> err) {
            return Result.err(new AnalyzeFailure.ChartDataUnavailable(err.error()));
        }

        Chart chart = ((Result.Ok<Chart, ChartFetchFailure>) chartResult).value();
        List<Candle> candles = chart.candlesForAnalysisAt(request.entryAt());

        AnalyzeResult analyzed;
        try {
            analyzed = AnalysisEngine.analyze(candles, direction);
        } catch (IllegalArgumentException ex) {
            return Result.err(new AnalyzeFailure.InvalidInput(ex.getMessage()));
        }

        if (authenticatedUserId != null) {
            analysisResultRepository.save(authenticatedUserId, request, analyzed);
        }

        return Result.ok(analyzed);
    }

    private Result<AnalyzeDirection, AnalyzeFailure> resolveDirection(BigDecimal entryPrice, BigDecimal stopLossPrice,
            BigDecimal takeProfitPrice) {
        if (isLess(stopLossPrice, entryPrice) && isGreater(takeProfitPrice, entryPrice)) {
            return Result.ok(AnalyzeDirection.LONG);
        }

        if (isGreater(stopLossPrice, entryPrice) && isLess(takeProfitPrice, entryPrice)) {
            return Result.ok(AnalyzeDirection.SHORT);
        }

        return Result.err(new AnalyzeFailure.InvalidInput(
                "cannot determine direction from entry/stopLoss/takeProfit prices"));
    }

    private boolean isLess(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) < 0;
    }

    private boolean isGreater(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) > 0;
    }
}
