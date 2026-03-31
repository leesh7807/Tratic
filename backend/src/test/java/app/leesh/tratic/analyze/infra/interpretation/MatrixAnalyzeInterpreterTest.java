package app.leesh.tratic.analyze.infra.interpretation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.leesh.tratic.analyze.domain.AnalyzeDirection;
import app.leesh.tratic.analyze.domain.AnalyzeResult;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeSignalLabels;
import app.leesh.tratic.analyze.domain.interpretation.AnalyzeScenario;

public class MatrixAnalyzeInterpreterTest {

    @Test
    @DisplayName("기본 정책에서 상승 정렬 상황은 지속 시나리오로 해석한다")
    public void interpret_maps_aligned_bullish_context_to_continuation_scenario() {
        MatrixAnalyzeInterpreter interpreter = new MatrixAnalyzeInterpreter(MatrixInterpretationPolicy.defaultPolicy());

        AnalyzeInterpretation interpretation = interpreter.interpret(new AnalyzeResult(
                AnalyzeDirection.LONG,
                55.0,
                70.0,
                72.0,
                45.0,
                0.45,
                0.31));

        assertEquals(AnalyzeScenario.BULLISH_TREND_CONTINUATION, interpretation.scenario());
        assertEquals(new AnalyzeSignalLabels("BULL", "EXPANSION", "UPPER_RANGE", "BUY"), interpretation.signals());
        assertEquals("matrix-v1", interpretation.policyVersion());
    }

    @Test
    @DisplayName("기본 정책에서 상단 반대 압력은 분배 경계 시나리오로 해석한다")
    public void interpret_changes_scenario_when_same_trend_faces_opposing_pressure_at_top() {
        MatrixAnalyzeInterpreter interpreter = new MatrixAnalyzeInterpreter(MatrixInterpretationPolicy.defaultPolicy());

        AnalyzeInterpretation interpretation = interpreter.interpret(new AnalyzeResult(
                AnalyzeDirection.LONG,
                55.0,
                70.0,
                88.0,
                -35.0,
                -0.35,
                -0.18));

        assertEquals(AnalyzeScenario.DISTRIBUTION_WARNING, interpretation.scenario());
        assertEquals(new AnalyzeSignalLabels("BULL", "EXPANSION", "PREMIUM", "SELL"), interpretation.signals());
    }

    @Test
    @DisplayName("기본 정책에서 분류되지 않는 상황은 비결정 폴백 시나리오를 사용한다")
    public void interpret_uses_fallback_scenario_for_unclassified_context() {
        MatrixAnalyzeInterpreter interpreter = new MatrixAnalyzeInterpreter(MatrixInterpretationPolicy.defaultPolicy());

        AnalyzeInterpretation interpretation = interpreter.interpret(new AnalyzeResult(
                AnalyzeDirection.LONG,
                85.0,
                10.0,
                50.0,
                85.0,
                0.85,
                0.70));

        assertEquals(AnalyzeScenario.INDECISIVE_TRANSITION, interpretation.scenario());
        assertEquals(new AnalyzeSignalLabels("STRONG_BULL", "CALM", "MID_RANGE", "STRONG_BUY"),
                interpretation.signals());
    }
}
