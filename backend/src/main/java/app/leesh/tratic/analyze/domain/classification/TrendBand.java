package app.leesh.tratic.analyze.domain.classification;

public enum TrendBand {
    STRONG_BEAR("강한 하락"),
    BEAR("하락"),
    NEUTRAL("중립"),
    BULL("상승"),
    STRONG_BULL("강한 상승");

    private final String displayKo;

    TrendBand(String displayKo) {
        this.displayKo = displayKo;
    }

    public String displayKo() {
        return displayKo;
    }
}
