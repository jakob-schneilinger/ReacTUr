package com.reactur.backend.security;

import com.reactur.backend.endpoints.options.ChemicalTypeEndpoint;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.options.ChemicalTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the baseline security headers configured in {@link SecurityConfig#filterChain} are
 * actually present on real responses - both authenticated and anonymous, since Spring Security's
 * headers filter is expected to apply regardless of authentication outcome. Uses
 * {@link ChemicalTypeEndpoint} purely as a convenient, already-existing endpoint to drive requests
 * through the real {@link SecurityConfig} filter chain - this test is about the headers, not that
 * endpoint's own behavior (already covered elsewhere).
 */
@WebMvcTest(ChemicalTypeEndpoint.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChemicalTypeService chemicalTypeService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String EXPECTED_CSP = "default-src 'none'; frame-ancestors 'self'";

    @Test
    void authenticatedResponse_includesBaselineContentSecurityPolicy() throws Exception {
        mockMvc.perform(get("/api/chemical_type")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", EXPECTED_CSP));
    }

    @Test
    void anonymousResponse_stillIncludesBaselineContentSecurityPolicy() throws Exception {
        mockMvc.perform(get("/api/chemical_type"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Security-Policy", EXPECTED_CSP));
    }

    @Test
    void authenticatedResponse_stillIncludesFrameOptions() throws Exception {
        mockMvc.perform(get("/api/chemical_type")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }
}
