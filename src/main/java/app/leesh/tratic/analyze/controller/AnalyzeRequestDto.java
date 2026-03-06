package app.leesh.tratic.analyze.controller;

import java.math.BigDecimal;
import java.time.Instant;

import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.TimeResolution;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnalyzeRequestDto(
        @NotNull Market market,
        @NotBlank String symbol,
        @NotNull TimeResolution resolution,
        @NotNull Instant entryAt,
        @NotNull @DecimalMin("0.0") BigDecimal entryPrice,
        @NotNull @DecimalMin("0.0") BigDecimal stopLossPrice,
        @NotNull @DecimalMin("0.0") BigDecimal takeProfitPrice,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal positionPct) {
}
