package com.reactur.backend.security;

import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import com.reactur.backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for {@link CustomJwtAuthenticationConverter}, with the repository mocked: covers
 * new-user sync on first login, granting the stored DB role rather than trusting the token, and
 * forcing UNAUTHORIZED for a deactivated user regardless of their stored role.
 */
@ExtendWith(MockitoExtension.class)
class CustomJwtAuthenticationConverterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomJwtAuthenticationConverter converter;

    private static final String KEYCLOAK_ID = "keycloak-subject-1";

    private Jwt jwtFor() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", KEYCLOAK_ID)
                .claim("email", "someone@example.com")
                .claim("given_name", "Some")
                .claim("family_name", "One")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }

    // Collects a converted token's granted authority names into a plain, guaranteed non-null Set,
    // so the tests below can use simple, null-safe Set.contains(...)/equals(...) checks instead of
    // repeatedly dereferencing Authentication.getAuthorities()/GrantedAuthority.getAuthority(),
    // neither of which is annotated @NotNull on the Spring Security side. The assertNotNull here is
    // the one place that's actually checked; everything the tests do with the returned Set<String>
    // afterward is null-safe by construction.
    private Set<String> authorityNamesOf(AbstractAuthenticationToken token) {
        Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
        assertNotNull(authorities);
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    void convert_ShouldGrantStoredRole_WhenUserIsActive() {
        User user = User.builder().id(KEYCLOAK_ID).role(Role.ADMIN).deactivatedAt(null).build();
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.of(user));

        AbstractAuthenticationToken token = converter.convert(jwtFor());

        assertTrue(authorityNamesOf(token).contains("ROLE_ADMIN"));
    }

    @Test
    void convert_ShouldGrantUnauthorized_WhenUserIsDeactivated_RegardlessOfStoredRole() {
        // Regression test: archiveUser only ever set User.deactivatedAt. Nothing consulted that
        // field when granting authorities, so a deactivated ADMIN kept full API access for as
        // long as their token/session remained valid. Confirmed by review before this fix.
        User deactivatedAdmin = User.builder().id(KEYCLOAK_ID).role(Role.ADMIN).deactivatedAt(Instant.now()).build();
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.of(deactivatedAdmin));

        AbstractAuthenticationToken token = converter.convert(jwtFor());

        // Exactly one authority, and it's ROLE_UNAUTHORIZED. That's strictly stronger than checking
        // "contains UNAUTHORIZED" and "doesn't contain ADMIN" as two separate assertions.
        assertEquals(Set.of("ROLE_UNAUTHORIZED"), authorityNamesOf(token));
        // The stored role itself must be left untouched: archiving is reversible.
        assertEquals(Role.ADMIN, deactivatedAdmin.getRole());
        verify(userRepository, never()).save(any());
    }

    @Test
    void convert_ShouldSynchronizeNewUser_WithUnauthorizedRole_WhenNotYetInDatabase() {
        when(userRepository.findById(KEYCLOAK_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AbstractAuthenticationToken token = converter.convert(jwtFor());

        assertTrue(authorityNamesOf(token).contains("ROLE_UNAUTHORIZED"));
    }
}
