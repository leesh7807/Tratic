package app.leesh.tratic.auth.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import app.leesh.tratic.auth.domain.OAuthIdentity;
import app.leesh.tratic.auth.domain.OAuthProvider;
import app.leesh.tratic.user.domain.UserId;

@Service
public class CustomOidcUserService extends OidcUserService {
    private final OAuthAccountResolver oAuthUserResolver;

    public CustomOidcUserService(OAuthAccountResolver oAuthUserResolver) {
        this.oAuthUserResolver = oAuthUserResolver;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        // 시큐리티 구현체에 검증을 맡김
        OidcUser oidcUser = super.loadUser(userRequest);

        OAuthProvider oAuthProvider = OAuthProvider.fromRegistrationId(userRequest
                .getClientRegistration()
                .getRegistrationId());

        String sub = oidcUser.getSubject(); // OIDC sub
        String email = oidcUser.getEmail();
        String name = resolveNickname(oidcUser);

        OAuthIdentity oauthIdentity = new OAuthIdentity(oAuthProvider, sub);
        UserId userId = oAuthUserResolver.resolveOrCreate(oauthIdentity, name);

        // 세션에 저장될 principal은 "OAuth2User" 타입이면 충분.
        // 여기서는 attributes에 내부 userId를 넣어두고, 이후 @AuthenticationPrincipal에서 꺼내 쓰는 방식.
        Map<String, Object> mergedAttrs = new HashMap<>(oidcUser.getAttributes());
        mergedAttrs.put("userId", userId.value().toString());
        mergedAttrs.put("provider", oAuthProvider.name());
        mergedAttrs.put("sub", sub);
        mergedAttrs.put("nickname", name);
        if (email != null)
            mergedAttrs.put("email", email);

        return new DefaultOidcUser(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()) {
            @Override
            public Map<String, Object> getAttributes() {
                return mergedAttrs;
            }
        };
    }

    private String resolveNickname(OidcUser oidcUser) {
        Object nickname = oidcUser.getAttributes().get("nickname");
        if (nickname != null && !nickname.toString().isBlank()) {
            return nickname.toString();
        }

        if (oidcUser.getFullName() != null && !oidcUser.getFullName().isBlank()) {
            return oidcUser.getFullName();
        }

        if (oidcUser.getGivenName() != null && !oidcUser.getGivenName().isBlank()) {
            return oidcUser.getGivenName();
        }

        return "user";
    }
}
