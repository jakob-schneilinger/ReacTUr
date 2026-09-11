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

        // TODO-CLAUDE (race condition inventory, Family 2 shape but already backstopped - see
        // docs/Software_todo.md "Race condition inventory"): this is a check-then-save (find-or-create) over a
        // row that doesn't exist yet at check time, so in principle two concurrent first-ever requests from the
        // same brand-new Keycloak user could both miss findById and both attempt to insert. Unlike the other
        // Family 2 findings, this one is already protected: `id` is the User primary key (the Keycloak subject),
        // so the DB's own PK uniqueness constraint rejects the second insert. That surfaces as a transient
        // exception/500 for the losing concurrent request rather than silent data corruption - a real but
        // self-limiting failure mode, not something that needs a fix with the same urgency as the other findings.
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

        // Deactivated accounts (User.deactivatedAt != null, set via UserService.archiveUser) keep
        // their stored role in the DB - archiving is reversible and shouldn't discard it - but must
        // not keep acting on it. Since roles are read fresh from the DB on every request (see the
        // comment on User.role), granting UNAUTHORIZED here takes effect on the very next request
        // after deactivation, reusing the exact same "not yet approved" authorization path already
        // enforced everywhere else, without needing any token/session revocation machinery.
        // TODO-CLAUDE (race condition inventory, third category - fits neither Family 1 nor Family 2, see
        // docs/Software_todo.md "Race condition inventory" under the ReactantRepository.hasNameConflict item):
        // this re-read-every-request approach closes the race for any *new* request made after an archive
        // commits, but not for a request that already passed this convert() call (reading deactivatedAt == null,
        // getting granted its real role) a moment *before* a concurrent archiveUser() commits - that one request
        // keeps executing with the old, non-revoked role for the rest of its lifetime. This can't be closed with a
        // DB row lock the way the other findings in the inventory can: the authorization read here and the
        // eventual business write happen in two different transactions (and possibly two different requests
        // entirely), so there's no single row/transaction to lock across. Actually closing it needs real
        // session/token revocation infrastructure (e.g. a short-TTL authorization cache with explicit
        // invalidation on archive, or Keycloak-side token revocation), which was explicitly not built - see the
        // reasoning in this method's own comment above. Low severity in practice (the window is just the
        // in-flight duration of one request racing the exact commit instant of an admin's archive action).
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