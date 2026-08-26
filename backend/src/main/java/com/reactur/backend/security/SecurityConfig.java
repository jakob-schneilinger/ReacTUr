package com.reactur.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        // Baseline CSP for a pure JSON API (no server-rendered HTML views anywhere in this
                        // backend): default-src 'none' means nothing is allowed to load/execute at all,
                        // which is exactly appropriate here and needs no tuning as frontend origins change
                        // later. frame-ancestors 'self' intentionally matches (not tightens beyond) the
                        // frameOptions setting above, rather than introducing an inconsistent stricter rule.
                        // Known caveat: this also applies to the H2 console (dev/test profiles only), whose
                        // own UI relies on inline scripts/styles this policy will block - acceptable since
                        // it's internal dev tooling, not customer-facing, and not used in production.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'self'")))
                .authorizeHttpRequests(auth -> auth
                        // TODO: Remove these Claude clarifying comments, or keep them simplified
                        //  as documentation
                        // Self-lookup must stay available to ANY authenticated principal (including
                        // not-yet-approved UNAUTHORIZED users) - this is how the frontend determines a
                        // user's own role, including detecting "pending approval" (see the Angular
                        // AuthService.getLoggedInUser(), which calls this to build the current user).
                        .requestMatchers(HttpMethod.GET, "/api/users/*/role").authenticated()
                        // Everyone else must have been approved: UNAUTHORIZED users (freshly synced,
                        // pending approval) are blocked from reading or writing anything else until a
                        // Supervisor/Admin assigns them a real role.
                        .anyRequest().hasAnyRole("USER", "ADMIN", "SUPERVISOR")
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)
                ));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
