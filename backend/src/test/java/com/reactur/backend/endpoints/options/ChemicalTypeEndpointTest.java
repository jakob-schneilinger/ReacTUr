package com.reactur.backend.endpoints.options;

import com.reactur.backend.exceptions.GlobalExceptionHandler;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.security.SecurityConfig;
import com.reactur.backend.services.options.ChemicalTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice test for {@link ChemicalTypeEndpoint}, representative of all four
 * option-catalog endpoints since they share {@link
 * com.reactur.backend.endpoints.options.AbstractOptionsEndpoint}'s implementation. The service
 * layer is mocked, but the real security filter chain runs, so these tests cover the ADMIN-only
 * write restrictions and HTTP status mapping for each outcome.
 */
@WebMvcTest(ChemicalTypeEndpoint.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ChemicalTypeEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChemicalTypeService chemicalTypeService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final String BASE_URL = "/api/chemical_type";

    @Test
    void getKnownOptions_asUser_shouldReturnOk() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void anyEndpoint_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getKnownOptions_asAdmin_shouldReturnOk() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void createOptions_asUser_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType("application/json")
                        .content("{\"name\":\"Test Chemical\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOptions_asAdmin_shouldReturnOk() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType("application/json")
                        .content("{\"name\":\"Test Chemical\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated());
    }

    @Test
    void archiveOption_asUser_shouldReturnForbidden() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void archiveOption_asAdmin_shouldReturnOk() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void restoreOption_asUser_shouldReturnForbidden() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + UUID.randomUUID() + "/restore")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreOption_asAdmin_shouldReturnOk() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + UUID.randomUUID() + "/restore")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
