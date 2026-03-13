package app.leesh.tratic.analyze.infra.persistence;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.service.AnalysisResultRepository;
import app.leesh.tratic.analyze.service.AnalyzeRequest;

@Repository
public class AnalysisResultRepositoryJpaAdapter implements AnalysisResultRepository {
    private final AnalysisResultJpaRepository repository;
    private final Clock clock;

    public AnalysisResultRepositoryJpaAdapter(AnalysisResultJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void save(UUID userId, AnalyzeRequest request, AnalyzeResult result) {
        Instant now = clock.instant();

        repository.save(new AnalysisResultEntity(
                userId,
                request.market(),
                request.symbol(),
                request.entryAt(),
                request.entryPrice(),
                request.stopLossPrice(),
                request.takeProfitPrice(),
                request.positionPct(),
                result.direction(),
                result.trendScore(),
                result.volatilityScore(),
                result.volatilityLabel(),
                result.locationScore(),
                result.pressureScore(),
                result.pressureRaw(),
                result.pressureView(),
                now));
    }
}
