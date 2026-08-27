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
            log.info("New user synchronized from Keycloak: {}", newUser.getEmail());
            return userRepository.save(newUser);
        });

        // Deactivated accounts (User.deactivatedAt != null, set via UserService.archiveUser) keep
        // their stored role in the DB - archiving is reversible and shouldn't discard it - but must
        // not keep acting on it. Since roles are read fresh from the DB on every request (see the
        // comment on User.role), granting UNAUTHORIZED here takes effect on the very next request
        // after deactivation, reusing the exact same "not yet approved" authorization path already
        // enforced everywhere else, without needing any token/session revocation machinery.
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