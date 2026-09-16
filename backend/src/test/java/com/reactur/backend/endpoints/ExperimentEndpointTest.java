package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exceptions.ResourceConflictException;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import com.reactur.backend.exceptions.UnprocessableEntityException;
import com.reactur.backend.security.RoleExtractor;
import com.reactur.backend.security.SecurityConfig;
import com.reactur.backend.services.ExperimentHistoryService;
import com.reactur.backend.services.ExperimentService;
import com.reactur.backend.repositories.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExperimentEndpoint.class)
@Import({SecurityConfig.class, RoleExtractor.class})
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

  // Grants the minimum authority needed to pass the app's "approved user" security gate, matching
  // the role mapping most of these tests exercise.
  private static RequestPostProcessor asUser(String subject) {
    return jwt().jwt(builder -> builder.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Test
  void createExperiment_whenInputIsValid_returns201Created() throws Exception {
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

    when(experimentService.createExperiment(any(SaveExperimentDto.class), anyString(), any(Role.class)))
      .thenReturn(expectedResponseDto);

    mockMvc.perform(post("/api/experiment")
        .with(csrf())
        // Simulated Keycloak JWT claims
        .with(jwt().jwt(builder -> builder
          .subject("mock-keycloak-id-123")
          .claim("email", "chemist@tuwien.ac.at")
          .claim("given_name", "Marie")
          .claim("family_name", "Curie"))
          .authorities(new SimpleGrantedAuthority("ROLE_USER")))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))

      .andExpect(status().isCreated())

      .andExpect(jsonPath("$.uuid").value(newExperimentId.toString()))
      .andExpect(jsonPath("$.dois[0]").value("10.1000/rxn123"));

    // Extra check: Making sure that the service got the right DTO, that nothing got accidentally changed
    ArgumentCaptor<SaveExperimentDto> dtoCaptor = ArgumentCaptor.forClass(SaveExperimentDto.class);

    verify(experimentService).createExperiment(
      dtoCaptor.capture(),
      eq("mock-keycloak-id-123"),
      eq(Role.USER)
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
      null,
      UUID.randomUUID(),
      validMetadata,
      validMeasurement,
      List.of("10.1000/rxn123")
    );

    mockMvc.perform(post("/api/experiment")
        .with(csrf())
        .with(asUser("mock-keycloak-id-123"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequestDto)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createExperiment_whenDoiIsMalformed_returns400BadRequest() throws Exception {
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
      UUID.randomUUID(),
      UUID.randomUUID(),
      validMetadata,
      validMeasurement,
      List.of("not-a-real-doi") // missing the mandatory "10.<digits>/" prefix
    );

    mockMvc.perform(post("/api/experiment")
        .with(csrf())
        .with(asUser("mock-keycloak-id-123"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequestDto)))

      .andExpect(status().isBadRequest());

    verifyNoInteractions(experimentService);
  }

  @Test
  void createExperiment_whenServiceThrowsUnprocessableEntity_returns422() throws Exception {
    SaveExperimentDto validRequestDto = createDummySaveExperimentDto();

    when(experimentService.createExperiment(any(SaveExperimentDto.class), anyString(), any(Role.class)))
      .thenThrow(new UnprocessableEntityException("Semantic validation failed"));

    mockMvc.perform(post("/api/experiment")
        .with(csrf())
        .with(asUser("mock-keycloak-id-123"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
      .andExpect(status().isUnprocessableContent());
  }

  @Test
  void getExperiment_whenFoundAndAuthorized_returns200Ok() throws Exception {
    UUID experimentId = UUID.randomUUID();
    GetExperimentDto activeExperiment = new GetExperimentDto(experimentId, List.of("10.1000/rxn123"), null, null, null, null, null, null, null);

    when(experimentService.getExperiment(eq(experimentId), anyString(), any(Role.class)))
      .thenReturn(activeExperiment);

    mockMvc.perform(get("/api/experiment/" + experimentId)
        .with(asUser("random-user-id")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.dois[0]").value("10.1000/rxn123"));
  }

  @Test
  void getExperiment_whenServiceThrowsForbidden_returns403() throws Exception {
    UUID experimentId = UUID.randomUUID();

    when(experimentService.getExperiment(eq(experimentId), anyString(), any(Role.class)))
      .thenThrow(new AccessDeniedException("Forbidden"));

    mockMvc.perform(get("/api/experiment/" + experimentId)
        .with(asUser("snooping-user-id")))
      .andExpect(status().isForbidden());
  }

  @Test
  void getExperiment_whenServiceThrowsNotFound_returns404() throws Exception {
    UUID experimentId = UUID.randomUUID();

    when(experimentService.getExperiment(eq(experimentId), anyString(), any(Role.class)))
      .thenThrow(new ResourceNotFoundException("Not found"));

    mockMvc.perform(get("/api/experiment/" + experimentId)
        .with(asUser("any-user-id")))
      .andExpect(status().isNotFound());
  }


  @Test
  void getAuditLogs_whenFoundAndAuthorized_returns200OkWithHistory() throws Exception {
    UUID experimentId = UUID.randomUUID();
    ExperimentHistoryDto historyEntry = new ExperimentHistoryDto(
      "author-123", Instant.parse("2024-01-01T00:00:00Z"), "UPDATE", Map.of("temperature", "20.0 -> 25.0")
    );

    when(experimentHistoryService.getExperimentHistory(eq(experimentId), anyString(), any(Role.class)))
      .thenReturn(List.of(historyEntry));

    mockMvc.perform(get("/api/experiment/" + experimentId + "/history")
        .with(asUser("random-user-id")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].authorId").value("author-123"))
      .andExpect(jsonPath("$[0].commitType").value("UPDATE"));
  }

  @Test
  void getAuditLogs_whenServiceThrowsForbidden_returns403() throws Exception {
    UUID experimentId = UUID.randomUUID();

    when(experimentHistoryService.getExperimentHistory(eq(experimentId), anyString(), any(Role.class)))
      .thenThrow(new AccessDeniedException("Forbidden"));

    mockMvc.perform(get("/api/experiment/" + experimentId + "/history")
        .with(asUser("snooping-user-id")))
      .andExpect(status().isForbidden());
  }

  @Test
  void getAuditLogs_whenServiceThrowsNotFound_returns404() throws Exception {
    UUID experimentId = UUID.randomUUID();

    when(experimentHistoryService.getExperimentHistory(eq(experimentId), anyString(), any(Role.class)))
      .thenThrow(new ResourceNotFoundException("Not found"));

    mockMvc.perform(get("/api/experiment/" + experimentId + "/history")
        .with(asUser("any-user-id")))
      .andExpect(status().isNotFound());
  }

  @Test
  void getAuditLogs_whenAuthorizedAsAdmin_passesRequestingUserIdAndRoleToService() throws Exception {
    UUID experimentId = UUID.randomUUID();
    String adminId = "admin-user-777";

    when(experimentHistoryService.getExperimentHistory(experimentId, adminId, Role.ADMIN))
      .thenReturn(List.of());

    mockMvc.perform(get("/api/experiment/" + experimentId + "/history")
        .with(jwt().jwt(builder -> builder.subject(adminId)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));

    verify(experimentHistoryService).getExperimentHistory(experimentId, adminId, Role.ADMIN);
  }

  @Test
  void archiveExperiment_whenAuthorized_returns204NoContent() throws Exception {
    UUID experimentId = UUID.randomUUID();

    // Mock service method is empty by default, so we just perform the request
    mockMvc.perform(delete("/api/experiment/" + experimentId)
        .with(csrf())
        .with(asUser("creator-123")))
      .andExpect(status().isNoContent());

    verify(experimentService).archiveExperiment(experimentId, "creator-123", Role.USER);
  }

  @Test
  void archiveExperiment_whenServiceThrowsForbidden_returns403() throws Exception {
    UUID experimentId = UUID.randomUUID();

    doThrow(new AccessDeniedException("Forbidden"))
      .when(experimentService).archiveExperiment(eq(experimentId), anyString(), any(Role.class));

    mockMvc.perform(delete("/api/experiment/" + experimentId)
        .with(csrf())
        .with(asUser("snooper-000")))
      .andExpect(status().isForbidden());
  }

  @Test
  void archiveExperiment_whenServiceThrowsNotFound_returns404() throws Exception {
    UUID experimentId = UUID.randomUUID();

    doThrow(new ResourceNotFoundException("Not found"))
      .when(experimentService).archiveExperiment(eq(experimentId), anyString(), any(Role.class));

    mockMvc.perform(delete("/api/experiment/" + experimentId)
        .with(csrf())
        .with(asUser("any-user-id")))
      .andExpect(status().isNotFound());
  }

  @Test
  void restoreExperiment_whenAuthorized_returns200Ok() throws Exception {
    UUID experimentId = UUID.randomUUID();
    GetExperimentDto restoredExperiment = new GetExperimentDto(experimentId, List.of("10.1000/rxn123"), null, null, null, null, null, null, null);

    when(experimentService.restoreExperiment(eq(experimentId), anyString(), any(Role.class)))
      .thenReturn(restoredExperiment);

    mockMvc.perform(put("/api/experiment/" + experimentId + "/restore")
        .with(csrf())
        .with(asUser("scientist-456")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.dois[0]").value("10.1000/rxn123"));

    verify(experimentService).restoreExperiment(experimentId, "scientist-456", Role.USER);
  }

  @Test
  void restoreExperiment_whenServiceThrowsForbidden_returns403() throws Exception {
    UUID experimentId = UUID.randomUUID();

    when(experimentService.restoreExperiment(eq(experimentId), anyString(), any(Role.class)))
      .thenThrow(new AccessDeniedException("Forbidden"));

    mockMvc.perform(put("/api/experiment/" + experimentId + "/restore")
        .with(csrf())
        .with(asUser("snooper-000")))
      .andExpect(status().isForbidden());
  }

  @Test
  void restoreExperiment_whenServiceThrowsNotFound_returns404() throws Exception {
    UUID experimentId = UUID.randomUUID();

    when(experimentService.restoreExperiment(eq(experimentId), anyString(), any(Role.class)))
      .thenThrow(new ResourceNotFoundException("Not found"));

    mockMvc.perform(put("/api/experiment/" + experimentId + "/restore")
        .with(csrf())
        .with(asUser("any-user-id")))
      .andExpect(status().isNotFound());
  }

  @Test
  void getArchivedExperiments_whenAuthorizedAsUser_returns200OkWithData() throws Exception {
    String userId = "standard-user-123";
    GetExperimentDto experiment1 = new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn001"), null, null, null, null, null, null, null);
    GetExperimentDto experiment2 = new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn002"), null, null, null, null, null, null, null);
    GetExperimentDto[] mockedResponse = { experiment1, experiment2 };

    when(experimentService.getArchivedExperiments(eq(userId), eq(Role.USER)))
      .thenReturn(mockedResponse);

    mockMvc.perform(get("/api/experiment/archived")
        .with(asUser(userId)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].dois[0]").value("10.1000/rxn001"))
      .andExpect(jsonPath("$[1].dois[0]").value("10.1000/rxn002"));

    verify(experimentService).getArchivedExperiments(userId, Role.USER);
  }

  @Test
  void getArchivedExperiments_whenAuthorizedAsAdmin_returns200OkWithData() throws Exception {
    String adminId = "admin-user-999";
    GetExperimentDto[] mockedResponse = {
      new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn999"), null, null, null, null, null, null, null)
    };

    when(experimentService.getArchivedExperiments(eq(adminId), eq(Role.ADMIN)))
      .thenReturn(mockedResponse);

    mockMvc.perform(get("/api/experiment/archived")
        .with(jwt().jwt(builder -> builder.subject(adminId)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].dois[0]").value("10.1000/rxn999"));

    verify(experimentService).getArchivedExperiments(adminId, Role.ADMIN);
  }

  @Test
  void getArchivedExperiments_whenEmpty_returns200OkWithEmptyArray() throws Exception {
    String userId = "lonely-user-000";
    GetExperimentDto[] mockedResponse = new GetExperimentDto[0];

    when(experimentService.getArchivedExperiments(eq(userId), eq(Role.USER)))
      .thenReturn(mockedResponse);

    mockMvc.perform(get("/api/experiment/archived")
        .with(asUser(userId)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));

    verify(experimentService).getArchivedExperiments(userId, Role.USER);
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

    when(experimentService.updateExperiment(
      eq(experimentId),
      any(SaveExperimentDto.class),
      anyString(),
      any(Role.class)
    )).thenReturn(expectedResponseDto);

    mockMvc.perform(put("/api/experiment/" + experimentId)
        .with(csrf())
        .with(asUser("mock-keycloak-id-123"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.uuid").value(experimentId.toString()))
      .andExpect(jsonPath("$.dois[0]").value("10.1000/rxn-updated"));

    verify(experimentService).updateExperiment(
      eq(experimentId),
      any(SaveExperimentDto.class),
      eq("mock-keycloak-id-123"),
      eq(Role.USER)
    );
  }

  @Test
  void updateExperiment_whenServiceThrowsForbidden_returns403() throws Exception {
    UUID experimentId = UUID.randomUUID();
    SaveExperimentDto validRequestDto = createDummySaveExperimentDto();

    when(experimentService.updateExperiment(
      eq(experimentId),
      any(SaveExperimentDto.class),
      anyString(),
      any(Role.class)
    )).thenThrow(new AccessDeniedException("Forbidden"));

    mockMvc.perform(put("/api/experiment/" + experimentId)
        .with(csrf())
        .with(asUser("snooper-123"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
      .andExpect(status().isForbidden());
  }

  @Test
  void updateExperiment_whenServiceThrowsNotFound_returns404() throws Exception {
    UUID experimentId = UUID.randomUUID();
    SaveExperimentDto validRequestDto = createDummySaveExperimentDto();

    when(experimentService.updateExperiment(
      eq(experimentId),
      any(SaveExperimentDto.class),
      anyString(),
      any(Role.class)
    )).thenThrow(new ResourceNotFoundException("Not found"));

    mockMvc.perform(put("/api/experiment/" + experimentId)
        .with(csrf())
        .with(asUser("any-user-id"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
      .andExpect(status().isNotFound());
  }

  @Test
  void updateExperiment_whenServiceThrowsConflict_returns409() throws Exception {
    UUID experimentId = UUID.randomUUID();
    SaveExperimentDto validRequestDto = createDummySaveExperimentDto();

    when(experimentService.updateExperiment(
      eq(experimentId),
      any(SaveExperimentDto.class),
      anyString(),
      any(Role.class)
    )).thenThrow(new ResourceConflictException("Cannot update an archived experiment"));

    mockMvc.perform(put("/api/experiment/" + experimentId)
        .with(csrf())
        .with(asUser("any-user-id"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
      .andExpect(status().isConflict());
  }

  @Test
  void searchExperiments_whenValidFiltersAndPagination_returns200OkWithPage() throws Exception {
    GetExperimentDto experiment1 = new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn1"), null, null, null, null, null, null, null);
    GetExperimentDto experiment2 = new GetExperimentDto(UUID.randomUUID(), List.of("10.1000/rxn2"), null, null, null, null, null, null, null);

    // mock Spring Page containing our data
    Page<GetExperimentDto> mockPage =
      new PageImpl<>(
        List.of(experiment1, experiment2),
        PageRequest.of(0, 20),
        2 // Total elements
      );

    when(experimentService.searchExperiments(
        any(SearchParametersDto.class),
        any(Pageable.class),
        anyString(),
        any(Role.class)
      )) .thenReturn(mockPage);

    mockMvc.perform(get("/api/experiment")
        .param("temperatureMin", "20.0")
        .param("temperatureMax", "30.0")
        .param("measurementSearch.rateConstantMin", "5.0") // Testing nested object mapping
        .param("page", "0")
        .param("size", "20")
        .with(asUser("mock-keycloak-id-123")))
      .andExpect(status().isOk())
      // Verify Spring Page JSON structure
      .andExpect(jsonPath("$.content.length()").value(2))
      .andExpect(jsonPath("$.content[0].dois[0]").value("10.1000/rxn1"))
      .andExpect(jsonPath("$.totalElements").value(2))
      .andExpect(jsonPath("$.pageable.pageSize").value(20));

    ArgumentCaptor<SearchParametersDto> dtoCaptor = ArgumentCaptor.forClass(SearchParametersDto.class);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    verify(experimentService).searchExperiments(
      dtoCaptor.capture(),
      pageableCaptor.capture(),
      eq("mock-keycloak-id-123"),
      eq(Role.USER)
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
    // A negative concentration fails the @PositiveOrZero validation constraint.
    mockMvc.perform(get("/api/experiment")
        .param("concentrationA_Min", "-5.0")
        .with(asUser("mock-keycloak-id-123")))
      .andExpect(status().isBadRequest());

    verify(experimentService, never()).searchExperiments(any(), any(), any(), any());
  }

  @Test
  void anyEndpoint_whenUnauthenticated_returns401() throws Exception {
    // A request with no JWT must be rejected before it reaches the controller.
    UUID experimentId = UUID.randomUUID();

    mockMvc.perform(get("/api/experiment/" + experimentId))
      .andExpect(status().isUnauthorized());
  }

  private SaveExperimentDto createDummySaveExperimentDto() {
    SaveMetadataDto metadata = new SaveMetadataDto(
      LocalDate.now(), "scientist-id", 25.0, UUID.randomUUID(), 1.0, 1.0, null, null, null, null
    );
    MeasurementDataDto measurement = new MeasurementDataDto(null, 1.0, 0.1, false);
    return new SaveExperimentDto(UUID.randomUUID(), UUID.randomUUID(), metadata, measurement, List.of("10.1000/rxn"));
  }

}
