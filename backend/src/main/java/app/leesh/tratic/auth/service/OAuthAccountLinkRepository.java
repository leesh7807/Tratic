package app.leesh.tratic.auth.service;

import java.util.Optional;

import app.leesh.tratic.auth.domain.OAuthIdentity;
import app.leesh.tratic.user.domain.UserId;

public interface OAuthAccountLinkRepository {

    Optional<UserId> findUserIdByOAuthIdentity(OAuthIdentity oAuthIdentity);

    /**
     * 이미 링크가 있으면 기존 UserId 반환.
     * 없으면 새 User 생성 + 링크 생성 후 UserId 반환.
     */
    UserId createNewUserAndLinkOAuth(OAuthIdentity oAuthIdentity, String nickname);
}
