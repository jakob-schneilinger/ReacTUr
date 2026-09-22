package com.reactur.backend.config;

import org.javers.spring.auditable.AuthorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;


/**
 * Configures Javers, the audit-trail library used to record entity change history.
 */
@Configuration
public class JaversConfig {

    /**
     * Identifies the author of a Javers commit as the current request's authenticated Keycloak
     * subject, falling back to {@code "system"} for changes made outside an HTTP request (e.g. a
     * scheduled job).
     *
     * @return the configured author provider
     */
    @Bean
    public AuthorProvider provideJaversAuthor() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // Extracts the Keycloak subject (string user ID) from the JWT token
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                return jwt.getSubject();
            }
            return "system"; // Fallback
        };
    }
}
