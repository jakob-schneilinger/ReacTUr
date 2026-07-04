package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.security.SecurityConfig;
import com.reactur.backend.services.ExperimentService;
import com.reactur.backend.repositories.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExperimentEndpoint.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(org.springframework.boot.h2console.autoconfigure.H2ConsoleProperties.class)
class ExperimentEndpointTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ExperimentService experimentService;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @Test
  void createExperiment_whenInputIsValid_returns201Created() throws Exception {

    // ## Data to send and to expect to be returned
    UUID reactantA = UUID.randomUUID();
    UUID reactantB = UUID.randomUUID();
    UUID newExperimentId = UUID.randomUUID();

    SaveMetadataDto validMetadata = new SaveMetadataDto(
      LocalDate.now().minusDays(1), // Yesterday (Valid)
      "mock-scientist-id-456",      // Scientist String ID
      25.0,                         // Temperature
      UUID.randomUUID(),            // Solvent ID
      1.5,                          // Concentration A (Positive)
      2.0,                          // Concentration B (Positive)
      UUID.randomUUID(),            // Instrument
      UUID.randomUUID(),            // Modality
      450.0,                        // Wavelength
      "Successful standard run"     // Comment
    );

    MeasurementDataDto validMeasurement = new MeasurementDataDto(
      1.2, 5.5, 0.1, false
    );

    SaveExperimentDto validRequestDto = new SaveExperimentDto(
      reactantA, reactantB, validMetadata, validMeasurement, "10.1000/rxn123"
    );

    GetExperimentDto expectedResponseDto = new GetExperimentDto(
      newExperimentId, "10.1000/rxn123", null, null, null, null, null, null, null
    );

    // ## Mocking of the service layer to accept the DTO and return the expected answer
    Mockito.when(experimentService.createExperiment(any(SaveExperimentDto.class), Mockito.anyString(), any(Role.class)))
      .thenReturn(expectedResponseDto);


    // ## Http call with the security tokens, and verifying the answer
    mockMvc.perform(post("/api/experiment")
        // CSRF token (required for POST requests)
        .with(csrf())
        // Keycloak JWT Mock for how UserSyncFilter expects it
        .with(jwt().jwt(builder -> builder
          .subject("mock-keycloak-id-123")
          .claim("email", "chemist@tuwien.ac.at")
          .claim("given_name", "Marie")
          .claim("family_name", "Curie")))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))

      .andExpect(status().isCreated())

      .andExpect(jsonPath("$.uuid").value(newExperimentId.toString()))
      .andExpect(jsonPath("$.doi").value("10.1000/rxn123"));

    // ## Extra check: Making sure that the service got the right DTO, that nothing got accidentally changed
    ArgumentCaptor<SaveExperimentDto> dtoCaptor = ArgumentCaptor.forClass(SaveExperimentDto.class);

    Mockito.verify(experimentService).createExperiment(
      dtoCaptor.capture(),
      Mockito.eq("mock-keycloak-id-123"),
      Mockito.eq(Role.USER)
    );
    SaveExperimentDto capturedDto = dtoCaptor.getValue();

    Assertions.assertNotNull(capturedDto, "The captured DTO should not be null");
    Assertions.assertEquals(reactantA, capturedDto.reactantA());
    Assertions.assertEquals(reactantB, capturedDto.reactantB());
    Assertions.assertEquals("10.1000/rxn123", capturedDto.doi());
    Assertions.assertEquals(25.0, capturedDto.metadata().temperature());
  }

  @Test
  void createExperiment_whenReactantAIsMissing_returns400BadRequest() throws Exception {

    SaveMetadataDto validMetadata = new SaveMetadataDto(
      LocalDate.now(),
      "mock-scientist-id-456",
      25.0,
      UUID.randomUUID(),
      1.5,
      2.0,
      UUID.randomUUID(),
      UUID.randomUUID(),
      450.0,
      "Test"
    );
    MeasurementDataDto validMeasurement = new MeasurementDataDto(
      1.2, 5.5, 0.1, false
    );

    SaveExperimentDto invalidRequestDto = new SaveExperimentDto(
      null, // <---
      UUID.randomUUID(),
      validMetadata,
      validMeasurement,
      "10.1000/rxn123"
    );

    mockMvc.perform(post("/api/experiment")
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject("mock-keycloak-id-123")))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequestDto)))

      .andExpect(status().isBadRequest());

    Mockito.verifyNoInteractions(experimentService);
  }

  @Test
  void getExperiment_whenFoundAndAuthorized_returns200Ok() throws Exception {
    UUID experimentId = UUID.randomUUID();
    GetExperimentDto activeExperiment = new GetExperimentDto(experimentId, "10.1000/rxn123", null, null, null, null, null, null, null);

    Mockito.when(experimentService.getExperiment(Mockito.eq(experimentId), Mockito.anyString(), any(Role.class)))
      .thenReturn(activeExperiment);

    mockMvc.perform(get("/api/experiment/" + experimentId)
        .with(jwt().jwt(builder -> builder.subject("random-user-id"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.doi").value("10.1000/rxn123"));
  }

  @Test
  void getExperiment_whenServiceThrowsForbidden_returns403() throws Exception {
    UUID experimentId = UUID.randomUUID();

    Mockito.when(experimentService.getExperiment(Mockito.eq(experimentId), Mockito.anyString(), any(Role.class)))
      .thenThrow(new AccessDeniedException("Forbidden"));

    mockMvc.perform(get("/api/experiment/" + experimentId)
        .with(jwt().jwt(builder -> builder.subject("snooping-user-id"))))
      .andExpect(status().isForbidden());
  }

  @Test
  void getExperiment_whenServiceThrowsNotFound_returns404() throws Exception {
    UUID experimentId = UUID.randomUUID();

    Mockito.when(experimentService.getExperiment(Mockito.eq(experimentId), Mockito.anyString(), any(Role.class)))
      .thenThrow(new ResourceNotFoundException("Not found"));

    mockMvc.perform(get("/api/experiment/" + experimentId)
        .with(jwt().jwt(builder -> builder.subject("any-user-id"))))
      .andExpect(status().isNotFound());
  }


  @Test
  void archiveExperiment_whenAuthorized_returns204NoContent() throws Exception {
    UUID experimentId = UUID.randomUUID();

    // The service method is void, so we just perform the request
    mockMvc.perform(delete("/api/experiment/" + experimentId)
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject("creator-123"))))
      .andExpect(status().isNoContent());

    Mockito.verify(experimentService).archiveExperiment(experimentId, "creator-123", Role.USER);
  }

  @Test
  void archiveExperiment_whenServiceThrowsForbidden_returns403() throws Exception {
    UUID experimentId = UUID.randomUUID();

    // Mock the void method to throw our exception
    Mockito.doThrow(new AccessDeniedException("Forbidden"))
      .when(experimentService).archiveExperiment(Mockito.eq(experimentId), Mockito.anyString(), any(Role.class));

    mockMvc.perform(delete("/api/experiment/" + experimentId)
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject("snooper-000"))))
      .andExpect(status().isForbidden());
  }

  @Test
  void restoreExperiment_whenAuthorized_returns200Ok() throws Exception {
    UUID experimentId = UUID.randomUUID();
    GetExperimentDto restoredExperiment = new GetExperimentDto(experimentId, "10.1000/rxn123", null, null, null, null, null, null, null);

    Mockito.when(experimentService.restoreExperiment(Mockito.eq(experimentId), Mockito.anyString(), any(Role.class)))
      .thenReturn(restoredExperiment);

    mockMvc.perform(put("/api/experiment/" + experimentId + "/restore")
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject("scientist-456"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.doi").value("10.1000/rxn123"));

    Mockito.verify(experimentService).restoreExperiment(experimentId, "scientist-456", Role.USER);
  }

  @Test
  void restoreExperiment_whenServiceThrowsForbidden_returns403() throws Exception {
    UUID experimentId = UUID.randomUUID();

    Mockito.when(experimentService.restoreExperiment(Mockito.eq(experimentId), Mockito.anyString(), any(Role.class)))
      .thenThrow(new AccessDeniedException("Forbidden"));

    mockMvc.perform(put("/api/experiment/" + experimentId + "/restore")
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject("snooper-000"))))
      .andExpect(status().isForbidden());
  }

}
