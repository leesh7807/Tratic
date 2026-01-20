package app.leesh.tratic.user.domain;

import java.util.UUID;

public record UserId(
        UUID value) {
    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
