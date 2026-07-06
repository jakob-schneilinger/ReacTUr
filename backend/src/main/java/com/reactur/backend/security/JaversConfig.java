package com.reactur.backend.security;

import org.javers.spring.auditable.AuthorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;


@Configuration
public class JaversConfig {

    @Bean
    public AuthorProvider provideJaversAuthor() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // Holt die Keycloak String ID aus dem JWT Token
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                return jwt.getSubject();
            }
            return "system"; // Fallback
        };
    }
}
