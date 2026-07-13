package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.security.SecurityConfig;
import com.reactur.backend.services.ExperimentHistoryService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
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

  @MockitoBean
  private ExperimentHistoryService experimentHistoryService;

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
      reactantA, reactantB, validMetadata, validMeasurement, List.of("10.1000/rxn123")
    );

    GetExperimentDto expectedResponseDto = new GetExperimentDto(
      newExperimentId, List.of("10.1000/rxn123"), null, null, null, null, null, null, null
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
      .andExpect(jsonPath("$.dois[0]").value("10.1000/rxn123"));

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
    Assertions.assertEquals(List.of("10.1000/rxn123"), capturedDto.dois());
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
      List.of("10.1000/rxn123")
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
    GetExperimentDto activeExperiment = new GetExperimentDto(experimentId, List.of("10.1000/rxn123"), null, null, null, null, null, null, null);

    Mockito.when(experimentService.getExperiment(Mockito.eq(experimentId), Mockito.anyString(), any(Role.class)))
      .thenReturn(activeExperiment);

    mockMvc.perform(get("/api/experiment/" + experimentId)
        .with(jwt().jwt(builder -> builder.subject("random-user-id"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.dois[0]").value("10.1000/rxn123"));
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
    GetExperimentDto restoredExperiment = new GetExperimentDto(experimentId, List.of("10.1000/rxn123"), null, null, null, null, null, null, null);

    Mockito.when(experimentService.restoreExperiment(Mockito.eq(experimentId), Mockito.anyString(), any(Role.class)))
      .thenReturn(restoredExperiment);

    mockMvc.perform(put("/api/experiment/" + experimentId + "/restore")
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject("scientist-456"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.dois[0]").value("10.1000/rxn123"));

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

  @Test
  void getArchivedExperiments_whenAuthorizedAsUser_returns200OkWithData() throws Exception {
    String userId = "standard-user-123";
    GetExperimentDto experiment1 = new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn001"), null, null, null, null, null, null, null);
    GetExperimentDto experiment2 = new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn002"), null, null, null, null, null, null, null);
    GetExperimentDto[] mockedResponse = { experiment1, experiment2 };

    Mockito.when(experimentService.getArchivedExperiments(Mockito.eq(userId), Mockito.eq(Role.USER)))
      .thenReturn(mockedResponse);

    mockMvc.perform(get("/api/experiment/archived")
        .with(jwt().jwt(builder -> builder.subject(userId))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].dois[0]").value("10.1000/rxn001"))
      .andExpect(jsonPath("$[1].dois[0]").value("10.1000/rxn002"));

    Mockito.verify(experimentService).getArchivedExperiments(userId, Role.USER);
  }

  @Test
  void getArchivedExperiments_whenAuthorizedAsAdmin_returns200OkWithData() throws Exception {
    String adminId = "admin-user-999";
    GetExperimentDto[] mockedResponse = {
      new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn999"), null, null, null, null, null, null, null)
    };

    Mockito.when(experimentService.getArchivedExperiments(Mockito.eq(adminId), Mockito.eq(Role.ADMIN)))
      .thenReturn(mockedResponse);

    mockMvc.perform(get("/api/experiment/archived")
        .with(jwt().jwt(builder -> builder.subject(adminId)).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].dois[0]").value("10.1000/rxn999"));

    Mockito.verify(experimentService).getArchivedExperiments(adminId, Role.ADMIN);
  }

  @Test
  void getArchivedExperiments_whenEmpty_returns200OkWithEmptyArray() throws Exception {
    String userId = "lonely-user-000";
    GetExperimentDto[] mockedResponse = new GetExperimentDto[0];

    Mockito.when(experimentService.getArchivedExperiments(Mockito.eq(userId), Mockito.eq(Role.USER)))
      .thenReturn(mockedResponse);

    mockMvc.perform(get("/api/experiment/archived")
        .with(jwt().jwt(builder -> builder.subject(userId))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));

    Mockito.verify(experimentService).getArchivedExperiments(userId, Role.USER);
  }

  @Test
  void updateExperiment_whenInputIsValid_returns200Ok() throws Exception {
    UUID experimentId = UUID.randomUUID();
    UUID reactantA = UUID.randomUUID();
    UUID reactantB = UUID.randomUUID();

    SaveMetadataDto validMetadata = new SaveMetadataDto(
      LocalDate.now().minusDays(1),
      "mock-scientist-id-456",
      25.0,
      UUID.randomUUID(),
      1.5,
      2.0,
      UUID.randomUUID(),
      UUID.randomUUID(),
      450.0,
      "Updated comment"
    );
    MeasurementDataDto validMeasurement = new MeasurementDataDto(
      1.2, 5.5, 0.1, false
    );
    SaveExperimentDto validRequestDto = new SaveExperimentDto(
      reactantA, reactantB, validMetadata, validMeasurement, List.of("10.1000/rxn-updated")
    );

    GetExperimentDto expectedResponseDto = new GetExperimentDto(
      experimentId, List.of("10.1000/rxn-updated"), null, null, null, null, null, null, null
    );

    Mockito.when(experimentService.updateExperiment(
      Mockito.eq(experimentId),
      any(SaveExperimentDto.class),
      Mockito.anyString(),
      any(Role.class)
    )).thenReturn(expectedResponseDto);

    mockMvc.perform(put("/api/experiment/" + experimentId)
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject("mock-keycloak-id-123")))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.uuid").value(experimentId.toString()))
      .andExpect(jsonPath("$.dois[0]").value("10.1000/rxn-updated"));

    Mockito.verify(experimentService).updateExperiment(
      Mockito.eq(experimentId),
      any(SaveExperimentDto.class),
      Mockito.eq("mock-keycloak-id-123"),
      Mockito.eq(Role.USER)
    );
  }

  @Test
  void updateExperiment_whenServiceThrowsForbidden_returns403() throws Exception {
    UUID experimentId = UUID.randomUUID();
    SaveExperimentDto validRequestDto = createDummySaveExperimentDto();

    Mockito.when(experimentService.updateExperiment(
      Mockito.eq(experimentId),
      any(SaveExperimentDto.class),
      Mockito.anyString(),
      any(Role.class)
    )).thenThrow(new AccessDeniedException("Forbidden"));

    mockMvc.perform(put("/api/experiment/" + experimentId)
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject("snooper-123")))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
      .andExpect(status().isForbidden());
  }

  @Test
  void searchExperiments_whenValidFiltersAndPagination_returns200OkWithPage() throws Exception {
    // Setup expected data
    GetExperimentDto experiment1 = new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn1"), null, null, null, null, null, null, null);
    GetExperimentDto experiment2 = new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn2"), null, null, null, null, null, null, null);

    // mock Spring Page containing our data
    Page<GetExperimentDto> mockPage =
      new PageImpl<>(
        List.of(experiment1, experiment2),
        PageRequest.of(0, 20),
        2 // Total elements
      );

    // service call
    Mockito.when(experimentService.searchExperiments(
        any(SearchParametersDto.class),
        any(Pageable.class),
        Mockito.anyString(),
        any(Role.class)
      )) .thenReturn(mockPage);

    // HTTP GET request with query parameters simulating the frontend URL
    mockMvc.perform(get("/api/experiment")
        .param("temperatureMin", "20.0")
        .param("temperatureMax", "30.0")
        .param("measurementSearch.rateConstantMin", "5.0") // Testing nested object mapping
        .param("page", "0")
        .param("size", "20")
        .with(jwt().jwt(builder -> builder.subject("mock-keycloak-id-123"))))
      .andExpect(status().isOk())
      // Verify Spring Page JSON structure
      .andExpect(jsonPath("$.content.length()").value(2))
      .andExpect(jsonPath("$.content[0].dois[0]").value("10.1000/rxn1"))
      .andExpect(jsonPath("$.totalElements").value(2))
      .andExpect(jsonPath("$.pageable.pageSize").value(20));

    // 4. Verify the DTO was populated correctly by Spring's argument resolver
    ArgumentCaptor<SearchParametersDto> dtoCaptor = ArgumentCaptor.forClass(SearchParametersDto.class);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    Mockito.verify(experimentService).searchExperiments(
      dtoCaptor.capture(),
      pageableCaptor.capture(),
      Mockito.eq("mock-keycloak-id-123"),
      Mockito.eq(Role.USER)
    );

    SearchParametersDto capturedDto = dtoCaptor.getValue();
    Assertions.assertEquals(20.0, capturedDto.temperatureMin());
    Assertions.assertEquals(30.0, capturedDto.temperatureMax());
    Assertions.assertNotNull(capturedDto.measurementSearch(), "Nested DTO should be parsed");
    Assertions.assertEquals(5.0, capturedDto.measurementSearch().rateConstantMin());

    Pageable capturedPageable = pageableCaptor.getValue();
    Assertions.assertEquals(0, capturedPageable.getPageNumber());
    Assertions.assertEquals(20, capturedPageable.getPageSize());
  }

  @Test
  void searchExperiments_whenInvalidFilterProvided_returns400BadRequest() throws Exception {
    // Simulating the user sending a negative concentration (which fails our @PositiveOrZero validation)
    mockMvc.perform(get("/api/experiment")
        .param("concentrationA_Min", "-5.0")
        .with(jwt().jwt(builder -> builder.subject("mock-keycloak-id-123"))))
      .andExpect(status().isBadRequest());

    Mockito.verify(experimentService, Mockito.never()).searchExperiments(any(), any(), any(), any());
  }

  private SaveExperimentDto createDummySaveExperimentDto() {
    SaveMetadataDto metadata = new SaveMetadataDto(
      LocalDate.now(), "scientist-id", 25.0, UUID.randomUUID(), 1.0, 1.0, null, null, null, null
    );
    MeasurementDataDto measurement = new MeasurementDataDto(null, 1.0, 0.1, false);
    return new SaveExperimentDto(UUID.randomUUID(), UUID.randomUUID(), metadata, measurement, List.of("10.1000/rxn"));
  }

}
