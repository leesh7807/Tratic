package app.leesh.tratic.user.domain;

import java.time.Instant;

public class User {
    private final UserId id;
    private String nickname;
    private final Instant createdAt;

    public User(UserId id, String nickname, Instant createdAt) {
        this.id = id;
        this.nickname = nickname;
        this.createdAt = createdAt;
    }

    public UserId id() {
        return id;
    }

    public String nickname() {
        return nickname;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
