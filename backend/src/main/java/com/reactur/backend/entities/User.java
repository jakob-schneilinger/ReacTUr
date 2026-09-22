package com.reactur.backend.entities;

import com.reactur.backend.enums.Role;
import jakarta.persistence.*;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import java.time.Instant;

/**
 * An application user, synchronized from Keycloak on first login. The row is keyed by the
 * Keycloak subject claim rather than a generated identifier, so it can always be looked up
 * directly from a JWT's {@code sub} claim.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** The Keycloak subject claim (not a generated UUID), also used as the JWT lookup key. */
    @Id
    @Column(unique = true, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    /**
     * Read fresh from this column on every request, never trusted from the JWT itself. The
     * Keycloak token carries no role claim; it only identifies which user row to look up.
     */
    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable = false)
    @Builder.Default
    private Role role = Role.UNAUTHORIZED;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    /**
     * Optimistic-locking token. Prevents two concurrent role changes on the same user from
     * silently overwriting each other.
     */
    @Version
    private Long version;
}
