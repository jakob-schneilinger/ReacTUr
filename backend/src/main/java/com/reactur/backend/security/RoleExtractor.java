package com.reactur.backend.security;

import com.reactur.backend.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps the Spring Security {@link Authentication} granted authorities (as produced by
 * {@link CustomJwtAuthenticationConverter}) to the application's {@link Role} enum.
 * <p>
 * Centralized here so every endpoint that needs to pass a {@code requestingUserRole} into a
 * service method derives it identically.
 */
@Component
public class RoleExtractor {

    public Role extractRole(Authentication authentication) {
        if (authentication == null) {
            return Role.UNAUTHORIZED;
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (authorities.contains("ROLE_ADMIN")) return Role.ADMIN;
        if (authorities.contains("ROLE_SUPERVISOR")) return Role.SUPERVISOR;
        if (authorities.contains("ROLE_USER")) return Role.USER;

        return Role.UNAUTHORIZED;
    }
}
