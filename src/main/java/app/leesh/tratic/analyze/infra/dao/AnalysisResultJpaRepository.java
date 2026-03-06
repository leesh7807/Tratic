package app.leesh.tratic.analyze.infra.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import app.leesh.tratic.analyze.infra.entity.AnalysisResultEntity;

public interface AnalysisResultJpaRepository extends JpaRepository<AnalysisResultEntity, Long> {
}
