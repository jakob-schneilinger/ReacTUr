package com.reactur.backend.entities;

import com.reactur.backend.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    // TODO: Remove gemini's enum explanation:
    /*
    By default, Hibernate saves Java Enums as Integers (their ordinal position). If Role.USER is first and Role.ADMIN is second, it saves 0 and 1.

    The Danger: If someone later adds Role.MODERATOR to the top of the Enum list, the integers shift. Suddenly, all your normal users in the database are promoted to Moderators!

    The Fix: We must explicitly tell Hibernate to save the string text of the enum using @Enumerated(EnumType.STRING)
     */
    @Enumerated(EnumType.STRING)
    @Column(name="role")
    private Role role;

    /* TODO: Discuss how Roles are extracted:
        Currently, UserSyncFilter does not extract the Role from the Keycloak JWT.
        Are we reading roles from the token, or managing them purely in our DB?
    */

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
