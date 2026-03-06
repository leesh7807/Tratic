package app.leesh.tratic.analyze.domain;

public record AnalysisEngineParams(
        double epsilon,
        int trendN,
        int trendFastEma,
        int trendSlowEma,
        double atrFloorRatio,
        double atrFloorMin,
        int volatilityAtrPeriod,
        int volatilityZWindow,
        double volatilityZScoreScale,
        double volatilityHighThreshold,
        double volatilityLowThreshold,
        int locationWindow,
        int atrShortPeriod,
        int atrLongPeriod,
        int minimumExtraCandles,
        int pressureVolumeEmaPeriod,
        int pressureViewEmaPeriod,
        double pressurePosCloseWeight,
        double pressureBodyWeight,
        double pressureWickDiffWeight,
        double pressureVolumeWeightMin,
        double pressureVolumeWeightMax) {

    public AnalysisEngineParams {
        if (epsilon <= 0.0) {
            throw new IllegalArgumentException("epsilon must be positive");
        }
        if (trendN <= 0 || trendFastEma <= 0 || trendSlowEma <= 0) {
            throw new IllegalArgumentException("trend periods must be positive");
        }
        if (atrFloorRatio <= 0.0 || atrFloorMin <= 0.0) {
            throw new IllegalArgumentException("atr floor params must be positive");
        }
        if (volatilityAtrPeriod <= 0 || volatilityZWindow <= 0 || volatilityZScoreScale <= 0.0) {
            throw new IllegalArgumentException("volatility params must be positive");
        }
        if (locationWindow <= 0 || atrShortPeriod <= 0 || atrLongPeriod <= 0 || minimumExtraCandles < 0) {
            throw new IllegalArgumentException("window params are invalid");
        }
        if (pressureVolumeEmaPeriod <= 0 || pressureViewEmaPeriod <= 0) {
            throw new IllegalArgumentException("pressure ema periods must be positive");
        }
        if (pressureVolumeWeightMin <= 0.0 || pressureVolumeWeightMax < pressureVolumeWeightMin) {
            throw new IllegalArgumentException("pressure volume weight range is invalid");
        }
    }

    public int minimumRequiredCandles() {
        return atrLongPeriod + minimumExtraCandles;
    }
}
