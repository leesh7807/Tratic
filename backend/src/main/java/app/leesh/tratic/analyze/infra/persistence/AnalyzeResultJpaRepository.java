package app.leesh.tratic.analyze.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyzeResultJpaRepository extends JpaRepository<AnalyzeResultEntity, Long> {
}
