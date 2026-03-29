package app.leesh.tratic.analyze.infra.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeScenario;
import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.TimeResolution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_result", indexes = {
        @Index(name = "idx_analysis_result_user_id", columnList = "user_id")
})
public class AnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "market", nullable = false, length = 20)
    private Market market;

    @Column(name = "symbol", nullable = false, length = 50)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", nullable = false, length = 20)
    private TimeResolution resolution;

    @Column(name = "entry_at", nullable = false)
    private Instant entryAt;

    @Column(name = "entry_price", nullable = false, precision = 30, scale = 10)
    private BigDecimal entryPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private AnalyzeDirection direction;

    @Column(name = "trend_score", nullable = false)
    private double trendScore;

    @Column(name = "volatility_score", nullable = false)
    private double volatilityScore;

    @Column(name = "location_score", nullable = false)
    private double locationScore;

    @Column(name = "pressure_score", nullable = false)
    private double pressureScore;

    @Column(name = "pressure_raw", nullable = false)
    private double pressureRaw;

    @Column(name = "pressure_view", nullable = false)
    private double pressureView;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario", nullable = false, length = 60)
    private AnalyzeScenario scenario;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AnalysisResultEntity() {
    }

    public AnalysisResultEntity(UUID userId, Market market, String symbol, TimeResolution resolution, Instant entryAt,
            BigDecimal entryPrice,
            AnalyzeDirection direction,
            double trendScore, double volatilityScore, double locationScore,
            double pressureScore, double pressureRaw, double pressureView, AnalyzeScenario scenario,
            String policyVersion, Instant createdAt) {
        this.userId = userId;
        this.market = market;
        this.symbol = symbol;
        this.resolution = resolution;
        this.entryAt = entryAt;
        this.entryPrice = entryPrice;
        this.direction = direction;
        this.trendScore = trendScore;
        this.volatilityScore = volatilityScore;
        this.locationScore = locationScore;
        this.pressureScore = pressureScore;
        this.pressureRaw = pressureRaw;
        this.pressureView = pressureView;
        this.scenario = scenario;
        this.policyVersion = policyVersion;
        this.createdAt = createdAt;
    }
}
