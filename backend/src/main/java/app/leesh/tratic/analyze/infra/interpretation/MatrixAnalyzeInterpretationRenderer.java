package app.leesh.tratic.analyze.infra.interpretation;

import org.springframework.stereotype.Component;

import app.leesh.tratic.analyze.domain.interpretation.AnalyzeInterpretation;
import app.leesh.tratic.analyze.service.AnalyzeInterpretationRenderer;

@Component
public class MatrixAnalyzeInterpretationRenderer implements AnalyzeInterpretationRenderer {
    @Override
    public String render(AnalyzeInterpretation interpretation) {
        return switch (interpretation.scenario()) {
            case BULLISH_BREAKOUT_EXTENSION -> "지금은 상방 돌파 연장을 우선해서 보는 해석이 맞습니다.";
            case BEARISH_BREAKOUT_EXTENSION -> "지금은 하방 돌파 연장을 우선해서 보는 해석이 맞습니다.";
            case BULLISH_TREND_CONTINUATION -> "지금은 상방 지속 쪽에 무게를 두는 해석이 더 자연스럽습니다.";
            case BEARISH_TREND_CONTINUATION -> "지금은 하방 지속 쪽에 무게를 두는 해석이 더 자연스럽습니다.";
            case DISTRIBUTION_WARNING -> "지금은 추가 상승 기대보다 상단에서의 분배와 되돌림을 먼저 경계해야 합니다.";
            case ACCUMULATION_WARNING -> "지금은 추가 하락 추종보다 하단에서의 복원 가능성을 함께 열어두는 편이 맞습니다.";
            case FAILED_BREAKOUT_RISK -> "지금은 돌파 추종보다 실패 가능성을 먼저 경계해야 하는 구간입니다.";
            case RANGE_ROTATION -> "지금은 한쪽 방향을 강하게 보기보다 범위 안 회전 구간으로 읽는 편이 맞습니다.";
            case LOW_VOL_COMPRESSION -> "지금은 방향을 단정하기보다 압축 이후 이탈을 기다리는 해석이 맞습니다.";
            case INDECISIVE_TRANSITION -> "지금은 방향성을 서둘러 단정하지 말고 전이 구간으로 받아들이는 편이 안전합니다.";
        };
    }
}
