package app.leesh.tratic.analyze.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.domain.AnalysisEngine;
import app.leesh.tratic.analyze.domain.AnalysisEngineParams;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;
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
    private final AnalyzePolicy analyzePolicy;
    private final AnalysisEnginePolicy analysisEnginePolicy;
    private final AnalysisResultRepository analysisResultRepository;
    private final AnalyzeInterpreter analyzeInterpreter;

    public AnalyzeService(ChartService chartService, AnalyzePolicy analyzePolicy,
            AnalysisEnginePolicy analysisEnginePolicy,
            AnalysisResultRepository analysisResultRepository,
            AnalyzeInterpreter analyzeInterpreter) {
        this.chartService = chartService;
        this.analyzePolicy = analyzePolicy;
        this.analysisEnginePolicy = analysisEnginePolicy;
        this.analysisResultRepository = analysisResultRepository;
        this.analyzeInterpreter = analyzeInterpreter;
    }

    public Result<AnalyzeInterpretation, AnalyzeFailure> analyze(AnalyzeRequest request, UUID authenticatedUserId) {
        TimeResolution resolution = request.resolution();
        AnalysisEngineParams engineParams = analysisEnginePolicy.resolve(resolution);
        return resolveDirection(request.entryPrice(), request.stopLossPrice())
                .flatMap(direction -> collectCandles(request)
                        .flatMap(candles -> analyzeCandles(candles, direction, engineParams))
                        .map(analyzed -> persistAndInterpret(authenticatedUserId, request, analyzed)));
    }

    private Result<AnalyzeDirection, AnalyzeFailure> resolveDirection(BigDecimal entryPrice, BigDecimal stopLossPrice) {
        if (isLess(stopLossPrice, entryPrice)) {
            return Result.ok(AnalyzeDirection.LONG);
        }

        if (isGreater(stopLossPrice, entryPrice)) {
            return Result.ok(AnalyzeDirection.SHORT);
        }

        return Result.err(new AnalyzeFailure.InvalidInput(
                "cannot determine direction from entry/stopLoss prices"));
    }

    private Result<List<Candle>, AnalyzeFailure> collectCandles(AnalyzeRequest request) {
        TimeResolution resolution = request.resolution();
        ChartSignature signature = new ChartSignature(request.market(), new Symbol(request.symbol()), resolution);
        Instant asOf = request.entryAt().minus(resolution.toDuration());

        return chartService.collectChart(new ChartFetchRequest(signature, asOf, analyzePolicy.fetchCandleCount()))
                .mapError(cause -> (AnalyzeFailure) new AnalyzeFailure.ChartDataUnavailable(cause))
                .map(chart -> chart.candlesBeforeBucketOf(request.entryAt()));
    }

    private Result<AnalyzeResult, AnalyzeFailure> analyzeCandles(List<Candle> candles, AnalyzeDirection direction,
            AnalysisEngineParams engineParams) {
        int minimumCandles = AnalysisEngine.minimumRequiredCandles(engineParams);
        if (candles.size() < minimumCandles) {
            return Result.err(new AnalyzeFailure.InsufficientCandles(minimumCandles, candles.size()));
        }

        try {
            return Result.ok(AnalysisEngine.analyze(candles, direction, engineParams));
        } catch (IllegalArgumentException ex) {
            return Result.err(new AnalyzeFailure.InvalidInput(ex.getMessage()));
        }
    }

    private AnalyzeInterpretation persistAndInterpret(UUID authenticatedUserId, AnalyzeRequest request, AnalyzeResult analyzed) {
        AnalyzeInterpretation interpretation = analyzeInterpreter.interpret(analyzed);

        if (authenticatedUserId != null) {
            analysisResultRepository.save(authenticatedUserId, request, analyzed, interpretation);
        }

        return interpretation;
    }

    private boolean isLess(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) < 0;
    }

    private boolean isGreater(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) > 0;
    }
}
