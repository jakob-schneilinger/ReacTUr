package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exeptions.GlobalExceptionHandler;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.security.SecurityConfig;
import com.reactur.backend.services.ReactantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReactantEndpoint.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReactantEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReactantService reactantService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createReactant_WhenAdmin_ShouldReturn201() throws Exception {
        UUID newId = UUID.randomUUID();
        String userId = "keycloak-user-id";
        CreateReactantDto saveDto = new CreateReactantDto("C=C", UUID.randomUUID(), "Ethene", null, "Ethene", "74-85-1", "VGGSQFCHJZSIN-UHFFFAOYSA-N", null, null);

        ReactantDetailDto mockResponse = new ReactantDetailDto(newId, "C=C", null, "Ethene", null, "Ethene", "74-85-1", "VGGSQFCHJZSIN-UHFFFAOYSA-N", null, null, null, List.of());

        when(reactantService.createReactant(any(CreateReactantDto.class), eq(userId))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/reactant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDto))
                        .with(jwt().jwt(jwt -> jwt.subject(userId)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(newId.toString()))
                .andExpect(jsonPath("$.shortName").value("Ethene"));
    }

    @Test
    void createReactant_WhenUser_ShouldReturn403() throws Exception {
        CreateReactantDto saveDto = new CreateReactantDto("C=C", UUID.randomUUID(), "Ethene", null, "Ethene", "74-85-1", "VGGSQFCHJZSIN-UHFFFAOYSA-N", null, null);

        mockMvc.perform(post("/api/reactant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDto))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(reactantService, never()).createReactant(any(), any());
    }

    @Test
    void getReactantDetail_ShouldReturn200_WhenReactantExists() throws Exception {
        UUID id = UUID.randomUUID();
        ReactantDetailDto mockResponse = new ReactantDetailDto(id, "C", null, "Methane", null, "Methane", "74-82-8", "VNWKTOKETHGBQD-UHFFFAOYSA-N", null, null, null, List.of());

        when(reactantService.getReactantDetail(id)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/reactant/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortName").value("Methane"));
    }

    @Test
    void getReactantDetail_ShouldReturn404_WhenReactantNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(reactantService.getReactantDetail(id)).thenThrow(new ResourceNotFoundException("Reactant not found"));

        mockMvc.perform(get("/api/reactant/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllReactants_ShouldReturn200_WithList() throws Exception {
        ReactantDto dto = new ReactantDto(UUID.randomUUID(), "C", null, "Methane", "Methane", StructureFlag.PENDING);
        when(reactantService.getAllReactants()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/reactant")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortName").value("Methane"))
                .andExpect(jsonPath("$[0].flag").value("PENDING"));
    }

    @Test
    void archiveReactant_WhenAdmin_ShouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/reactant/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(reactantService, times(1)).archiveReactant(id);
    }

    @Test
    void archiveReactant_WhenUser_ShouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/reactant/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(reactantService, never()).archiveReactant(any());
    }
}