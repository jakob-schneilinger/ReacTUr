package com.reactur.backend.entities;

import com.reactur.backend.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import java.time.Instant;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(unique = true, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable = false)
    @Builder.Default
    private Role role = Role.UNAUTHORIZED;

    // Roles are always read fresh from this column on every request, via
    // CustomJwtAuthenticationConverter (wired into SecurityConfig as the JWT-to-Authentication
    // converter) - the Keycloak JWT itself carries no role claim, it's only used to identify which
    // user's DB row to look up. Intentional design, not an open question.

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;
}
