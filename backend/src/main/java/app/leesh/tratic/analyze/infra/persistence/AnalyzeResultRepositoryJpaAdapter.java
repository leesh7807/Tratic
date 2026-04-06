package app.leesh.tratic.analyze.infra.persistence;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.service.AnalyzeResultRepository;
import app.leesh.tratic.analyze.service.AnalyzeRequest;

@Repository
public class AnalyzeResultRepositoryJpaAdapter implements AnalyzeResultRepository {
    private final AnalyzeResultJpaRepository repository;
    private final Clock clock;

    public AnalyzeResultRepositoryJpaAdapter(AnalyzeResultJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void save(UUID userId, AnalyzeRequest request, AnalyzeResult result) {
        Instant now = clock.instant();

        repository.save(new AnalyzeResultEntity(
                userId,
                request.market(),
                request.symbol(),
                request.resolution(),
                request.entryAt(),
                request.entryPrice(),
                result.direction(),
                result.trendScore(),
                result.locationScore(),
                result.pressureScore(),
                now));
    }
}
