package com.reactur.backend.security;

import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import com.reactur.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        // User aus DB holen oder neu anlegen (wie in deinem Filter)
        User user = userRepository.findById(keycloakId).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(keycloakId);
            newUser.setEmail(jwt.getClaimAsString("email"));
            newUser.setFirstName(jwt.getClaimAsString("given_name"));
            newUser.setLastName(jwt.getClaimAsString("family_name"));
            // Hier weisen wir standardmäßig die Rolle UNAUTHORIZED oder USER zu
            newUser.setRole(Role.UNAUTHORIZED);
            System.out.println("Neuer User aus Keycloak synchronisiert: " + newUser.getEmail());
            return userRepository.save(newUser);
        });

        // HIER PASSIERT DIE MAGIE: Wir sagen Spring Security, welche Rolle der User hat!
        // Wichtig: Spring Security erwartet standardmäßig den Präfix "ROLE_"
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new JwtAuthenticationToken(jwt, authorities);
    }
}