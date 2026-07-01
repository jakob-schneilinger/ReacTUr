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

    /* TODO: Discuss how Roles are extracted:
        Currently, UserSyncFilter does not extract the Role from the Keycloak JWT.
        Are we reading roles from the token, or managing them purely in our DB?
    */

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;
}
