package app.leesh.tratic.analyze.infra.interpretation;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;
import app.leesh.tratic.analyze.service.AnalyzeInterpreter;

@Component
public class MatrixAnalyzeInterpreter implements AnalyzeInterpreter {
    private final MatrixInterpretationPolicy policy;

    public MatrixAnalyzeInterpreter() {
        this(MatrixInterpretationPolicy.defaultPolicy());
    }

    MatrixAnalyzeInterpreter(MatrixInterpretationPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    @Override
    public AnalyzeInterpretation interpret(AnalyzeResult result) {
        Objects.requireNonNull(result, "result must not be null");

        SignalSet signals = new SignalSet(
                classifyScore(result.trendScore(), policy.trendBands()),
                classifyScore(result.volatilityScore(), policy.volatilityBands()),
                classifyScore(result.locationScore(), policy.locationBands()),
                classifyScore(result.pressureScore(), policy.pressureBands()));

        MatrixAnalyzeScenarioRule scenario = policy.prioritizedScenarios().stream()
                .filter(rule -> rule.matches(
                        signals.trend(),
                        signals.volatility(),
                        signals.location(),
                        signals.pressure()))
                .findFirst()
                .orElse(policy.fallbackScenario());

        return new AnalyzeInterpretation(
                result.direction(),
                scenario.scenario(),
                scenario.bias(),
                scenario.confidence(),
                scenario.riskLevel(),
                policy.policyVersion());
    }

    private String classifyScore(double score, List<MatrixAnalyzeScoreBand> bands) {
        return bands.stream()
                .filter(band -> band.matches(score))
                .findFirst()
                .map(MatrixAnalyzeScoreBand::signal)
                .orElseThrow(() -> new IllegalArgumentException("no score band configured for score=" + score));
    }

    private record SignalSet(
            String trend,
            String volatility,
            String location,
            String pressure) {

        SignalSet {
            Objects.requireNonNull(trend, "trend must not be null");
            Objects.requireNonNull(volatility, "volatility must not be null");
            Objects.requireNonNull(location, "location must not be null");
            Objects.requireNonNull(pressure, "pressure must not be null");
        }
    }
}
