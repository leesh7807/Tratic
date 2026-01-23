package app.leesh.tratic.auth.infra.entity;

import java.util.UUID;

import app.leesh.tratic.auth.domain.OAuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "oauth_account_link", uniqueConstraints = {
        @UniqueConstraint(name = "uq_oauth_provider_subject", columnNames = { "provider", "subject" })
}, indexes = {
        @Index(name = "idx_oauth_provider_subject", columnList = "provider,subject")
})
public class OAuthAccountLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    protected OAuthAccountLinkEntity() {
    }

    public OAuthAccountLinkEntity(OAuthProvider provider, String subject, UUID userId) {
        this.provider = provider;
        this.subject = subject;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getSubject() {
        return subject;
    }

    public UUID getUserId() {
        return userId;
    }
}