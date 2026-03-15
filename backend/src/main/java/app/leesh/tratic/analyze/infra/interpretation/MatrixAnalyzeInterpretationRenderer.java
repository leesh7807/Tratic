package app.leesh.tratic.analyze.infra.interpretation;

import org.springframework.stereotype.Component;

import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;
import app.leesh.tratic.analyze.service.AnalyzeInterpretationRenderer;

@Component
public class MatrixAnalyzeInterpretationRenderer implements AnalyzeInterpretationRenderer {
    @Override
    public String render(AnalyzeInterpretation interpretation) {
        return switch (interpretation.scenario()) {
            case BULLISH_BREAKOUT_EXTENSION -> "강한 상승 추세와 확장 변동성이 동반되어 상방 돌파 연장 가능성이 열려 있는 구간입니다.";
            case BEARISH_BREAKOUT_EXTENSION -> "강한 하락 추세와 확장 변동성이 동반되어 하방 돌파 연장 가능성이 열려 있는 구간입니다.";
            case BULLISH_TREND_CONTINUATION -> "상승 추세와 매수 압력이 정렬되어 상방 지속 관점이 우세한 구간입니다.";
            case BEARISH_TREND_CONTINUATION -> "하락 추세와 매도 압력이 정렬되어 하방 지속 관점이 우세한 구간입니다.";
            case DISTRIBUTION_WARNING -> "상단 위치에서 추세 대비 수급이 약화되어 분배 또는 되돌림 경계가 필요한 구간입니다.";
            case ACCUMULATION_WARNING -> "하단 위치에서 하락 추세 대비 매수 압력이 붙어 반대 방향 복원 가능성이 생기는 구간입니다.";
            case FAILED_BREAKOUT_RISK -> "추세와 위치는 연장 구간처럼 보이지만 수급이 동행하지 않아 실패 돌파 위험이 큰 구간입니다.";
            case RANGE_ROTATION -> "추세 우위가 약하고 범위 내부에서 회전하는 성격이 강한 구간입니다.";
            case LOW_VOL_COMPRESSION -> "변동성이 낮아 방향성 확정보다 압축 구간으로 해석하는 편이 안전합니다.";
            case INDECISIVE_TRANSITION -> "축 신호가 혼재되어 방향성과 연속성을 단정하기 어려운 전이 구간입니다.";
        };
    }
}
