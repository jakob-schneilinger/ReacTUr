package com.reactur.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
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

/**
 * Configures the application's security filter chains, CORS policy, and JWT-based
 * authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    /**
     * Security filter chain for the Swagger/OpenAPI documentation endpoints. Evaluated first
     * ({@code @Order(1)}) since it claims only those specific paths via {@code securityMatcher};
     * every other request falls through to {@link #filterChain}. Permits all requests: the API
     * documentation itself isn't sensitive, and the endpoints it describes still enforce their
     * own authorization independently.
     *
     * @param http the security configuration builder
     * @return the built filter chain
     * @throws Exception if the security configuration is invalid
     */
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        http
          .securityMatcher("/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html")
          .csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .headers(headers -> headers
            .contentSecurityPolicy(csp -> csp.policyDirectives(
              "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")));

        return http.build();
    }

    /**
     * Security filter chain for every other request: stateless, JWT-bearer-token authentication
     * via {@link CustomJwtAuthenticationConverter}. Any approved role may call most endpoints;
     * per-endpoint/method restrictions on top of that are declared with {@code @PreAuthorize}
     * where needed.
     *
     * @param http the security configuration builder
     * @return the built filter chain
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
          .csrf(AbstractHttpConfigurer::disable)
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .headers(headers -> headers
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            .contentSecurityPolicy(csp -> csp.policyDirectives(
              "default-src 'none'; frame-ancestors 'self'")))
          .authorizeHttpRequests(auth -> auth

            // Self-lookup stays available to ANY authenticated principal, including
            // not-yet-approved UNAUTHORIZED users: this way, the frontend determines a
            // user's own role, including detecting "pending approval".
            .requestMatchers(HttpMethod.GET, "/api/users/*/role").authenticated()

            // ...everyone else must have been approved: UNAUTHORIZED users (freshly synced,
            // pending approval) are blocked from reading or writing anything else until a
            // Supervisor/Admin assigns them a real role.
            .anyRequest().hasAnyRole("USER", "ADMIN", "SUPERVISOR"))
          .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
            jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)
          ));

        return http.build();
    }

    /**
     * Allows the frontend's dev-server origin to call this API from a browser.
     *
     * @return the CORS configuration source
     */
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
