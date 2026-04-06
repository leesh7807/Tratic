package app.leesh.tratic.analyze.domain.classification;

public enum LocationBand {
    LOWEST("낮은 자리"),
    LOWER("하단"),
    MIDDLE("중앙"),
    UPPER("상단"),
    HIGHEST("높은 자리");

    private final String displayKo;

    LocationBand(String displayKo) {
        this.displayKo = displayKo;
    }

    public String displayKo() {
        return displayKo;
    }
}
