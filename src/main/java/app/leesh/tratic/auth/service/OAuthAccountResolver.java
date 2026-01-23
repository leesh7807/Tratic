package app.leesh.tratic.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.leesh.tratic.auth.domain.OAuthIdentity;
import app.leesh.tratic.user.domain.UserId;

@Service
public class OAuthAccountResolver {
    private final OAuthAccountLinkRepository oAuthUserLinkRepository;

    public OAuthAccountResolver(OAuthAccountLinkRepository oAuthUserLinkRepository) {
        this.oAuthUserLinkRepository = oAuthUserLinkRepository;
    }

    @Transactional
    public UserId resolveOrCreate(OAuthIdentity oAuthIdentity, String nickname) {
        return oAuthUserLinkRepository.findUserIdByOAuthIdentity(oAuthIdentity)
                .orElseGet(() -> oAuthUserLinkRepository.createNewUserAndLinkOAuth(oAuthIdentity, nickname));
    }

}
