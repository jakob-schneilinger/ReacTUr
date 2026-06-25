package com.reactur.backend.entities;

import com.reactur.backend.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(unique = true, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String email;

    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable = false)
    private Role role = Role.UNAUTHORIZED;

    @Column(updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
}
