package com.reactur.backend;

import com.reactur.backend.dtos.ReactantDtos.CreateReactantDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.endpoints.ReactantEndpoint;
import com.reactur.backend.exeptions.GlobalExceptionHandler;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.ReactantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ReactantEndpoint.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(GlobalExceptionHandler.class)
class ReactantEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReactantService reactantService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void createReactant_ShouldReturn201_WhenSuccessful() throws Exception {
        UUID newId = UUID.randomUUID();
        CreateReactantDto saveDto = new CreateReactantDto("C=C", UUID.randomUUID(), "Ethene", null, "Ethene", "74-85-1", "VGGSQFCHJZSIN-UHFFFAOYSA-N", null, null);
        ReactantDetailDto mockResponse = new ReactantDetailDto(newId, "C=C", null, "Ethene", null, "Ethene", "74-85-1", "VGGSQFCHJZSIN-UHFFFAOYSA-N", null, null, null, null);

        when(reactantService.createReactant(any(CreateReactantDto.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/reactant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(newId.toString()))
                .andExpect(jsonPath("$.shortName").value("Ethene"));
    }

    @Test
    void getReactantDetail_ShouldReturn200_WhenReactantExists() throws Exception {
        UUID id = UUID.randomUUID();
        ReactantDetailDto mockResponse = new ReactantDetailDto(id, "C", null, "Methane", null, "Methane", "74-82-8", "VNWKTOKETHGBQD-UHFFFAOYSA-N", null, null, null, null);

        when(reactantService.getReactantDetail(id)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/reactant/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortName").value("Methane"));
    }

    @Test
    void getReactantDetail_ShouldReturn404_WhenReactantNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(reactantService.getReactantDetail(id)).thenThrow(new ResourceNotFoundException("Reactant not found"));

        mockMvc.perform(get("/api/reactant/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllReactants_ShouldReturn200_WithList() throws Exception {
        ReactantDto dto = new ReactantDto(UUID.randomUUID(), "C", null, "Methane", "Methane", null);
        when(reactantService.getAllReactants()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/reactant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortName").value("Methane"));
    }

    @Test
    void archiveReactant_ShouldReturn204_WhenSuccessful() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/reactant/" + id))
                .andExpect(status().isNoContent());
    }
}