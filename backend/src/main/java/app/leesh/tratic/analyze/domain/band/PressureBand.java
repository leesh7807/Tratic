package app.leesh.tratic.analyze.domain.band;

public enum PressureBand {
    STRONG_SELL("강한 매도"),
    SELL("매도 우세"),
    NEUTRAL("중립"),
    BUY("매수 우세"),
    STRONG_BUY("강한 매수");

    private final String displayKo;

    PressureBand(String displayKo) {
        this.displayKo = displayKo;
    }

    public String displayKo() {
        return displayKo;
    }
}
