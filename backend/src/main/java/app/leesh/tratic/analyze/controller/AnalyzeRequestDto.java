package app.leesh.tratic.analyze.controller;

import java.math.BigDecimal;
import java.time.Instant;

import app.leesh.tratic.chart.domain.Market;
import app.leesh.tratic.chart.domain.TimeResolution;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record AnalyzeRequestDto(
        @Schema(description = "Exchange market", example = "BINANCE")
        @NotNull Market market,
        @Schema(description = "Market symbol", example = "BTCUSDT")
        @NotBlank String symbol,
        @Schema(description = "Chart resolution", example = "M15")
        @NotNull TimeResolution resolution,
        @Schema(description = "Trade entry timestamp in ISO-8601 UTC", example = "2026-01-10T10:00:00Z")
        @NotNull Instant entryAt,
        @Schema(description = "Entry price", example = "100.0")
        @NotNull @DecimalMin("0.0") BigDecimal entryPrice,
        @Schema(description = "Stop loss price", example = "90.0")
        @NotNull @DecimalMin("0.0") BigDecimal stopLossPrice,
        @Schema(description = "Take profit price", example = "110.0")
        @NotNull @DecimalMin("0.0") BigDecimal takeProfitPrice,
        @Schema(description = "Position size percentage", example = "25.5")
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal positionPct) {
}
