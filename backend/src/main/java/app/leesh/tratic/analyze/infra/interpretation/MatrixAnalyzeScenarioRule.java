package app.leesh.tratic.analyze.infra.interpretation;

import java.util.List;
import java.util.Objects;

import app.leesh.tratic.analyze.domain.interpretation.AnalyzeScenario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record MatrixAnalyzeScenarioRule(
        AnalyzeScenario scenario,
        @NotBlank String bias,
        @NotBlank String confidence,
        @NotBlank String riskLevel,
        @NotEmpty List<@NotBlank String> trend,
        @NotEmpty List<@NotBlank String> volatility,
        @NotEmpty List<@NotBlank String> location,
        @NotEmpty List<@NotBlank String> pressure) {

    public MatrixAnalyzeScenarioRule {
        Objects.requireNonNull(scenario, "scenario must not be null");
        trend = immutableCopy(trend, "trend");
        volatility = immutableCopy(volatility, "volatility");
        location = immutableCopy(location, "location");
        pressure = immutableCopy(pressure, "pressure");
    }

    public boolean matches(String trendSignal, String volatilitySignal, String locationSignal, String pressureSignal) {
        return trend.contains(trendSignal)
                && volatility.contains(volatilitySignal)
                && location.contains(locationSignal)
                && pressure.contains(pressureSignal);
    }

    private static List<String> immutableCopy(List<String> source, String fieldName) {
        Objects.requireNonNull(source, fieldName + " must not be null");
        if (source.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return List.copyOf(source);
    }
}
