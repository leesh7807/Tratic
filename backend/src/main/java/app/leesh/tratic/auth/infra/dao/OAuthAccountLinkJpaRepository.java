package app.leesh.tratic.auth.infra.dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.leesh.tratic.auth.domain.OAuthProvider;
import app.leesh.tratic.auth.infra.entity.OAuthAccountLinkEntity;

public interface OAuthAccountLinkJpaRepository extends JpaRepository<OAuthAccountLinkEntity, Long> {

    @Query("""
            select l.userId
            from OAuthAccountLinkEntity l
            where l.provider = :provider and l.subject = :subject
            """)
    Optional<UUID> findUserIdByProviderAndSubject(@Param("provider") OAuthProvider provider,
            @Param("subject") String subject);
}