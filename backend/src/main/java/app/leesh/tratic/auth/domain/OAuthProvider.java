package app.leesh.tratic.auth.domain;

public enum OAuthProvider {
    GOOGLE,
    KAKAO;

    // OAuth2UserRequest에 담겨있는 id기준
    public static OAuthProvider fromRegistrationId(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> GOOGLE;
            case "kakao" -> KAKAO;
            default -> throw new IllegalArgumentException("unknown provider: " + registrationId);
        };
    }
}
