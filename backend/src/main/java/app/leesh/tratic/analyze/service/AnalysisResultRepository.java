package app.leesh.tratic.analyze.service;

import java.util.UUID;

import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;

public interface AnalysisResultRepository {
    void save(UUID userId, AnalyzeRequest request, AnalyzeResult result, AnalyzeInterpretation interpretation);
}
