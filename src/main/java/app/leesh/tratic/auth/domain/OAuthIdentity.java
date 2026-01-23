package app.leesh.tratic.auth.domain;

import java.util.Objects;

public record OAuthIdentity(OAuthProvider provider, String sub) {
    public OAuthIdentity {
        Objects.requireNonNull(provider, "provider is required");
        Objects.requireNonNull(sub, "sub is required");
    }
}
