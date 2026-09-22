package com.reactur.backend.security;

import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import com.reactur.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    /**
     * Resolves an incoming JWT's subject into this application's actual authorization: looks up
     * (or creates, on first login) the corresponding {@link User} row by Keycloak ID, and grants
     * a single {@code ROLE_*} authority based on that row's current, database-stored role rather
     * than anything carried in the token itself. The token carries no role claim at all; it only
     * identifies which user to look up.
     * <p>
     * A brand-new user is created with {@link Role#UNAUTHORIZED} and must be promoted by an
     * ADMIN/SUPERVISOR before gaining real access. A deactivated user is granted {@code
     * UNAUTHORIZED} regardless of their stored role, which is what actually enforces
     * deactivation: since roles are read fresh on every request, this takes effect on the very
     * next request after deactivation, with no token or session revocation needed.
     *
     * @param jwt the decoded JWT for the current request
     * @return an {@link AbstractAuthenticationToken} carrying exactly one {@code ROLE_*} authority
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        User user = userRepository.findById(keycloakId).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(keycloakId);
            newUser.setEmail(jwt.getClaimAsString("email"));
            newUser.setFirstName(jwt.getClaimAsString("given_name"));
            newUser.setLastName(jwt.getClaimAsString("family_name"));
            newUser.setRole(Role.UNAUTHORIZED);
            log.info("New user synchronized from Keycloak: {}", keycloakId);
            return userRepository.save(newUser);
        });

        // Deactivated accounts keep their stored role in the DB (archiving is reversible) but must
        // not keep acting on it. Roles are read fresh on every request, so granting UNAUTHORIZED
        // here takes effect on the very next request after deactivation, without needing any
        // token/session revocation machinery.
        Role effectiveRole = user.getRole();
        if (user.getDeactivatedAt() != null) {
            log.warn("Deactivated user {} attempted to authenticate; granting UNAUTHORIZED instead of stored role {}.",
                    keycloakId, user.getRole());
            effectiveRole = Role.UNAUTHORIZED;
        }

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + effectiveRole.name())
        );

        return new JwtAuthenticationToken(jwt, authorities);
    }
}