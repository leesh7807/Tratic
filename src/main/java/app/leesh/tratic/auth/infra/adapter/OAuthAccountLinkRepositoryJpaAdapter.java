package app.leesh.tratic.auth.infra.adapter;

import java.time.Instant;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import app.leesh.tratic.auth.domain.OAuthIdentity;
import app.leesh.tratic.auth.infra.dao.OAuthAccountLinkJpaRepository;
import app.leesh.tratic.auth.infra.entity.OAuthAccountLinkEntity;
import app.leesh.tratic.auth.service.OAuthAccountLinkRepository;
import app.leesh.tratic.user.domain.UserId;
import app.leesh.tratic.user.infra.dao.UserJpaRepository;
import app.leesh.tratic.user.infra.entity.UserEntity;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OAuthAccountLinkRepositoryJpaAdapter implements OAuthAccountLinkRepository {

    private final UserJpaRepository userRepo;
    private final OAuthAccountLinkJpaRepository linkRepo;

    @Override
    public Optional<UserId> findUserIdByOAuthIdentity(OAuthIdentity identity) {
        return linkRepo.findUserIdByProviderAndSubject(identity.provider(), identity.sub())
                .map(UserId::new);
    }

    @Override
    public UserId createNewUserAndLinkOAuth(OAuthIdentity identity, String nickname) {

        // 유니크 위반 시 재조회
        UserId newUserId = UserId.newId();
        Instant now = Instant.now();

        try {
            userRepo.save(new UserEntity(newUserId.value(), nickname, now));
            linkRepo.save(new OAuthAccountLinkEntity(
                    identity.provider(),
                    identity.sub(),
                    newUserId.value()));
            return newUserId;

        } catch (DataIntegrityViolationException e) {
            return linkRepo.findUserIdByProviderAndSubject(
                    identity.provider(), identity.sub())
                    .map(UserId::new)
                    .orElseThrow(() -> e);
        }
    }
}