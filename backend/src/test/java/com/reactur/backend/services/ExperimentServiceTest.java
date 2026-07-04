package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.entities.User;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.exeptions.UnprocessableEntityException;
import com.reactur.backend.mappers.ExperimentMapper;
import com.reactur.backend.mappers.MeasurementDataMapper;
import com.reactur.backend.mappers.MetadataMapper;
import com.reactur.backend.mappers.ReactantMapper;
import com.reactur.backend.mappers.UserMapper;
import com.reactur.backend.mappers.options.InstrumentMapper;
import com.reactur.backend.mappers.options.ModalityMapper;
import com.reactur.backend.mappers.options.SolventMapper;
import com.reactur.backend.repositories.AuditLogRepository;
import com.reactur.backend.repositories.ExperimentRepository;

import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.implementations.ExperimentServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import com.reactur.backend.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import com.reactur.backend.entities.experiment.Metadata;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Tells JUnit to use Mockito instead of loading Spring Boot
class ExperimentServiceTest {

  @Mock private ExperimentRepository experimentRepository;
  @Mock private ReactantRepository reactantRepository;
  @Mock private SolventRepository solventRepository;
  @Mock private InstrumentRepository instrumentRepository;
  @Mock private ModalityRepository modalityRepository;
  @Mock private UserRepository userRepository;
  @Mock private AuditLogRepository auditLogRepository;

  // We actually want to use here the mapper rather than a Mock. This way we also test the Mapper.
  @Spy private ExperimentMapper experimentMapper = Mappers.getMapper(ExperimentMapper.class);
  @Spy private MetadataMapper metadataMapper = Mappers.getMapper(MetadataMapper.class);
  @Spy private MeasurementDataMapper measurementDataMapper = Mappers.getMapper(MeasurementDataMapper.class);
  @Spy private ReactantMapper reactantMapper = Mappers.getMapper(ReactantMapper.class);
  @Spy private UserMapper userMapper = Mappers.getMapper(UserMapper.class);
  @Spy private SolventMapper solventMapper = Mappers.getMapper(SolventMapper.class);
  @Spy private InstrumentMapper instrumentMapper = Mappers.getMapper(InstrumentMapper.class);
  @Spy private ModalityMapper modalityMapper = Mappers.getMapper(ModalityMapper.class);

  // Mockito will take the mocked Repositories
  // and inject them into this real Service instance.
  @InjectMocks
  private ExperimentServiceImpl experimentService;  // Because of the junit-mockito test,
                                                    // we cannot auto-wire the interface and need to specify the
                                                    // concrete class

  @BeforeEach
  void setUp() {
    // Manually wire the nested mappers into the corresponding mapper(s)

    ReflectionTestUtils.setField(metadataMapper, "userMapper", userMapper);
    ReflectionTestUtils.setField(metadataMapper, "solventMapper", solventMapper);
    ReflectionTestUtils.setField(metadataMapper, "instrumentMapper", instrumentMapper);
    ReflectionTestUtils.setField(metadataMapper, "modalityMapper", modalityMapper);

    ReflectionTestUtils.setField(experimentMapper, "metadataMapper", metadataMapper);
    ReflectionTestUtils.setField(experimentMapper, "measurementDataMapper", measurementDataMapper);
    ReflectionTestUtils.setField(experimentMapper, "reactantMapper", reactantMapper);
    ReflectionTestUtils.setField(experimentMapper, "userMapper", userMapper);
  }

  @Test
  void createExperiment_whenValidInput_returnsGetExperimentDto() {

    String userId = "mock-keycloak-id-123";
    String scientistId = "mock-scientist-id-456";
    UUID reactantAId = randomUUID();
    UUID reactantBId = randomUUID();
    UUID solventId = randomUUID();
    UUID instrumentId = randomUUID();
    UUID modalityId = randomUUID();

    SaveMetadataDto validMetadata = new SaveMetadataDto(
      java.time.LocalDate.now().minusDays(1),
      scientistId,
      25.0,
      solventId,
      1.5,
      2.0,
      instrumentId,
      modalityId,
      450.0,
      "Test"
    );
    MeasurementDataDto validMeasurement = new MeasurementDataDto(
      1.2, 5.5, 0.1, false
    );
    SaveExperimentDto validRequestDto = new SaveExperimentDto(
      reactantAId, reactantBId, validMetadata, validMeasurement, "10.1000/rxn123"
    );

    // ... create Mocks with the response-data:
    User mockCreator = new User();
    mockCreator.setId(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(mockCreator));

    User mockScientist = new User();
    mockScientist.setId(scientistId);
    when(userRepository.findById(scientistId)).thenReturn(Optional.of(mockScientist));

    Reactant mockReactantA = new Reactant();
    mockReactantA.setId(reactantAId);
    when(reactantRepository.findById(reactantAId)).thenReturn(Optional.of(mockReactantA));

    Reactant mockReactantB = new Reactant();
    mockReactantB.setId(reactantBId);
    when(reactantRepository.findById(reactantBId)).thenReturn(Optional.of(mockReactantB));

    Solvent mockSolvent = new Solvent();
    mockSolvent.setId(solventId);
    when(solventRepository.findById(solventId)).thenReturn(Optional.of(mockSolvent));

    Instrument mockInstrument = new Instrument();
    mockInstrument.setId(instrumentId);
    when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(mockInstrument));

    Modality mockModality = new Modality();
    mockModality.setId(modalityId);
    when(modalityRepository.findById(modalityId)).thenReturn(Optional.of(mockModality));

    // ... then return the entity of the "saved" experiment

    when(experimentRepository.save(any()))
      .thenAnswer(invocation -> {
        Experiment saved = invocation.getArgument(0);
        saved.setId(randomUUID()); // Simulates DB generating an ID
        return saved;
      });

    // ------------------

    GetExperimentDto result = experimentService.createExperiment(validRequestDto, userId, Role.USER);

    // Asserting the performed calls...

    ArgumentCaptor<Experiment> experimentCaptor = ArgumentCaptor.forClass(Experiment.class);

    Mockito.verify(experimentRepository).save(experimentCaptor.capture());
    Experiment capturedEntity = experimentCaptor.getValue();

    // Builder map correct relationships
    assertNotNull(capturedEntity, "The captured entity should not be null");
    assertEquals("10.1000/rxn123", capturedEntity.getDoi());
    assertEquals(reactantAId, capturedEntity.getReactantA().getId(), "Reactant A was not mapped correctly");
    assertEquals(solventId, capturedEntity.getMetadata().getSolvent().getId(), "Solvent was not mapped correctly");

    // Delegated creation logic:
    assertEquals(userId, capturedEntity.getCreatedBy().getId(), "Creator should be the token user");
    assertEquals(scientistId, capturedEntity.getMetadata().getScientist().getId(), "Scientist should be from the DTO");

    assertEquals(25.0, capturedEntity.getMetadata().getTemperature(), "Temperature was not mapped correctly");

    // Asserting the returned value (Mappers)
    assertNotNull(result, "The returned DTO should not be null");
    assertEquals("10.1000/rxn123", result.doi());

    assertNotNull(result.metadata(), "The returned metadata DTO should not be null");
    assertEquals(25.0, result.metadata().temperature());
  }

  @Test
  void createExperiment_whenCreatorNotFound_throwsIllegalStateException() {
    String invalidUserId = "non-existent-token-id";
    SaveExperimentDto validDto = createDummyExperimentDto();

    // Token user doesn't exist in the database
    when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> experimentService.createExperiment(validDto, invalidUserId, Role.USER)
    );

    assertTrue(exception.getMessage().contains("Authenticated user lacks internal profile"));

    Mockito.verify(experimentRepository, Mockito.never()).save(any());
  }

  @Test
  void createExperiment_whenScientistNotFound_throwsUnprocessableEntityException() {
    String validUserId = "mock-keycloak-id-123";
    SaveExperimentDto validDto = createDummyExperimentDto();

    // Token user exists, but the scientist in the DTO does not
    when(userRepository.findById(validUserId)).thenReturn(Optional.of(new User()));
    when(userRepository.findById(validDto.metadata().scientist())).thenReturn(Optional.empty());

    UnprocessableEntityException exception = assertThrows(
      UnprocessableEntityException.class,
      () -> experimentService.createExperiment(validDto, validUserId, Role.USER)
    );

    assertTrue(exception.getMessage().contains("Scientist with ID"));

    Mockito.verify(experimentRepository, Mockito.never()).save(any());
  }

  @Test
  void createExperiment_whenReactantIsMissing_throwsUnprocessableEntityException() {
    String validUserId = "mock-keycloak-id-123";
    SaveExperimentDto validDto = createDummyExperimentDto();

    // Reactant does not exist
    when(userRepository.findById(validUserId)).thenReturn(Optional.of(new User()));
    when(userRepository.findById(validDto.metadata().scientist())).thenReturn(Optional.of(new User()));
    when(reactantRepository.findById(validDto.reactantA())).thenReturn(Optional.empty());

    UnprocessableEntityException exception = assertThrows(
      UnprocessableEntityException.class,
      () -> experimentService.createExperiment(validDto, validUserId, Role.USER)
    );

    assertTrue(exception.getMessage().contains("Reactant A does not exist"));
    Mockito.verify(experimentRepository, Mockito.never()).save(any());
  }

  @Test
  void getExperiment_whenFound_returnsDto() {
    UUID experimentId = randomUUID();
    String requestingUserId = "user-123";

    Experiment mockExperiment = new Experiment();
    mockExperiment.setId(experimentId);
    mockExperiment.setDoi("10.1000/rxn999");

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    GetExperimentDto result = experimentService.getExperiment(experimentId, requestingUserId, Role.USER);

    assertNotNull(result, "The returned DTO should not be null");
    assertEquals(experimentId, result.uuid());
    assertEquals("10.1000/rxn999", result.doi());
  }

  @Test
  void getExperiment_whenNotFound_throwsResourceNotFoundException() {
    UUID experimentId = randomUUID();
    String requestingUserId = "user-123";

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(
      ResourceNotFoundException.class,
      () -> experimentService.getExperiment(experimentId, requestingUserId, Role.USER)
    );

    assertTrue(exception.getMessage().contains("Experiment with ID " + experimentId + " not found"));
  }

  @Test
  void getExperiment_whenArchivedAndUserIsCreator_returnsDto() {
    UUID experimentId = randomUUID();
    String creatorId = "creator-123";

    // Experiment IS archived
    Experiment mockExperiment = createMockEntityForSecurity(experimentId, creatorId, "scientist-456", Instant.now());
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    // Request is made by the original Creator
    GetExperimentDto result = experimentService.getExperiment(experimentId, creatorId, Role.USER);

    assertNotNull(result, "Creator should be able to view their archived experiment");
    assertEquals(experimentId, result.uuid());
  }

  @Test
  void getExperiment_whenArchivedAndUserIsScientist_returnsDto() {
    UUID experimentId = randomUUID();
    String scientistId = "scientist-456";

    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", scientistId, Instant.now());
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    // Request is made by the original Scientist
    GetExperimentDto result = experimentService.getExperiment(experimentId, scientistId, Role.USER);

    assertNotNull(result, "Scientist should be able to view their archived experiment");
  }

  @Test
  void getExperiment_whenArchivedAndUserIsAdmin_returnsDto() {
    UUID experimentId = randomUUID();

    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", Instant.now());
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    // Request is made by an Admin (different user ID)
    GetExperimentDto result = experimentService.getExperiment(experimentId, "admin-999", Role.ADMIN);

    assertNotNull(result, "Admins should be able to view any archived experiment");
  }

  @Test
  void getExperiment_whenArchivedAndUserIsForbidden_throwsAccessDeniedException() {
    UUID experimentId = randomUUID();

    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", Instant.now());
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    // Request is made by a standard user who has no relation to the experiment
    assertThrows(AccessDeniedException.class, () ->
      experimentService.getExperiment(experimentId, "snooper-000", Role.USER)
    );
  }

  @Test
  void archiveExperiment_whenUserIsCreator_archivesAndSaves() {
    UUID experimentId = randomUUID();
    String creatorId = "creator-123";

    Experiment mockExperiment = createMockEntityForSecurity(experimentId, creatorId, "scientist-456", null);
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    experimentService.archiveExperiment(experimentId, creatorId, Role.USER);

    assertNotNull(mockExperiment.getArchivedAt(), "Archived timestamp should be set");
    Mockito.verify(experimentRepository).save(mockExperiment);
    Mockito.verify(auditLogRepository).save(any());
  }

  @Test
  void archiveExperiment_whenUserIsAdmin_archivesAndSaves() {
    UUID experimentId = randomUUID();

    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", null);
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    experimentService.archiveExperiment(experimentId, "admin-999", Role.ADMIN);

    assertNotNull(mockExperiment.getArchivedAt());
    Mockito.verify(experimentRepository).save(mockExperiment);
  }

  @Test
  void archiveExperiment_whenUserIsForbidden_throwsAccessDeniedException() {
    UUID experimentId = randomUUID();

    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", null);
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    assertThrows(AccessDeniedException.class, () ->
      experimentService.archiveExperiment(experimentId, "snooper-000", Role.USER)
    );

    Mockito.verify(experimentRepository, Mockito.never()).save(any());
    Mockito.verify(auditLogRepository, Mockito.never()).save(any());
  }


  @Test
  void restoreExperiment_whenUserIsScientist_restoresAndSaves() {
    UUID experimentId = randomUUID();
    String scientistId = "scientist-456";

    // Experiment is CURRENTLY archived (Instant.now())
    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", scientistId, Instant.now());
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));
    when(experimentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    experimentService.restoreExperiment(experimentId, scientistId, Role.USER);

    Assertions.assertNull(mockExperiment.getArchivedAt(), "Archived timestamp should be cleared");
    Mockito.verify(experimentRepository).save(mockExperiment);
    Mockito.verify(auditLogRepository).save(any());
  }

  @Test
  void restoreExperiment_whenUserIsForbidden_throwsAccessDeniedException() {
    UUID experimentId = randomUUID();

    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", Instant.now());
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    assertThrows(AccessDeniedException.class, () ->
      experimentService.restoreExperiment(experimentId, "snooper-000", Role.USER)
    );

    Mockito.verify(experimentRepository, Mockito.never()).save(any());
  }


  private SaveExperimentDto createDummyExperimentDto() {
    SaveMetadataDto metadata = new SaveMetadataDto(
      java.time.LocalDate.now().minusDays(1),
      "mock-scientist-id-456",
      25.0,
      randomUUID(),
      1.5,
      2.0,
      randomUUID(),
      randomUUID(),
      450.0,
      "Test"
    );
    return new SaveExperimentDto(
      randomUUID(), randomUUID(), metadata, new MeasurementDataDto(1.2, 5.5, 0.1, false), "10.1000/rxn123"
    );
  }


  private Experiment createMockEntityForSecurity(UUID experimentId, String creatorId, String scientistId, Instant archivedAt) {
    User creator = new User();
    creator.setId(creatorId);

    User scientist = new User();
    scientist.setId(scientistId);

    Metadata metadata = Metadata.builder().scientist(scientist).build();

    return Experiment.builder()
      .id(experimentId)
      .createdBy(creator)
      .metadata(metadata)
      .archivedAt(archivedAt)
      .build();
  }


}
