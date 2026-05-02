package com.reactur.backend.Security;

import com.reactur.backend.entities.User;
import com.reactur.backend.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UserSyncFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakId = jwt.getSubject();
            if (userRepository.findById(keycloakId).isEmpty()) {
                User newUser = new User();
                newUser.setId(keycloakId);
                newUser.setEmail(jwt.getClaimAsString("email"));
                newUser.setFirstName(jwt.getClaimAsString("given_name"));
                newUser.setLastName(jwt.getClaimAsString("family_name"));
                userRepository.save(newUser);

                System.out.println("Neuer User aus Keycloak synchronisiert: " + newUser.getEmail());
            }
        }

        filterChain.doFilter(request, response);
    }
}
