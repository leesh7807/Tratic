package app.leesh.tratic.analyze.service;

import java.util.UUID;

import app.leesh.tratic.analyze.domain.AnalyzeResult;

public interface AnalyzeResultRepository {
    void save(UUID userId, AnalyzeRequest request, AnalyzeResult result);
}
