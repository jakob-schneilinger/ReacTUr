package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exceptions.ResourceConflictException;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import com.reactur.backend.exceptions.UnprocessableEntityException;
import com.reactur.backend.mappers.ExperimentAttachmentMapper;
import com.reactur.backend.mappers.ExperimentMapper;
import com.reactur.backend.mappers.MeasurementDataMapper;
import com.reactur.backend.mappers.MetadataMapper;
import com.reactur.backend.mappers.ReactantMapper;
import com.reactur.backend.mappers.UserMapper;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import com.reactur.backend.mappers.options.InstrumentMapper;
import com.reactur.backend.mappers.options.ModalityMapper;
import com.reactur.backend.mappers.options.SolventMapper;
import com.reactur.backend.repositories.ExperimentRepository;

import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.implementations.ExperimentServiceImpl;
import com.reactur.backend.validators.ExperimentValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Tells JUnit to use Mockito instead of loading Spring Boot
class ExperimentServiceTest {

  @Mock private ExperimentRepository experimentRepository;
  @Mock private ReactantRepository reactantRepository;
  @Mock private SolventRepository solventRepository;
  @Mock private InstrumentRepository instrumentRepository;
  @Mock private ModalityRepository modalityRepository;
  @Mock private UserRepository userRepository;
  @Mock private ReactantStructureRepository structureRepository;

  @Mock
  private FileService fileService;

  // We actually want to use here the mapper rather than a Mock. This way we also test the Mapper.
  @Spy private ExperimentMapper experimentMapper = Mappers.getMapper(ExperimentMapper.class);
  @Spy private MetadataMapper metadataMapper = Mappers.getMapper(MetadataMapper.class);
  @Spy private MeasurementDataMapper measurementDataMapper = Mappers.getMapper(MeasurementDataMapper.class);
  @Spy private ReactantMapper reactantMapper = Mappers.getMapper(ReactantMapper.class);
  @Spy private UserMapper userMapper = Mappers.getMapper(UserMapper.class);
  @Spy private SolventMapper solventMapper = Mappers.getMapper(SolventMapper.class);
  @Spy private InstrumentMapper instrumentMapper = Mappers.getMapper(InstrumentMapper.class);
  @Spy private ModalityMapper modalityMapper = Mappers.getMapper(ModalityMapper.class);
  @Spy private ExperimentAttachmentMapper experimentAttachmentMapper = Mappers.getMapper(ExperimentAttachmentMapper.class);

  @Spy private ExperimentAuthorization experimentAuthorization = new ExperimentAuthorization();

  // Mockito will take the mocked Repositories
  // and inject them into this real Service instance.
  @InjectMocks
  private ExperimentServiceImpl experimentService;  // Because of the junit-mockito test,
                                                    // we cannot auto-wire the interface and need to specify the
                                                    // concrete class

  @BeforeEach
  void setUp() {
    // Manual Wire of non-mocked dependencies

    ExperimentValidator experimentValidator = new ExperimentValidator(
      userRepository, reactantRepository, solventRepository, instrumentRepository, modalityRepository, structureRepository
    );
    ReflectionTestUtils.setField(experimentService, "experimentValidator", experimentValidator);

    ReflectionTestUtils.setField(metadataMapper, "userMapper", userMapper);
    ReflectionTestUtils.setField(metadataMapper, "solventMapper", solventMapper);
    ReflectionTestUtils.setField(metadataMapper, "instrumentMapper", instrumentMapper);
    ReflectionTestUtils.setField(metadataMapper, "modalityMapper", modalityMapper);

    ReflectionTestUtils.setField(experimentMapper, "metadataMapper", metadataMapper);
    ReflectionTestUtils.setField(experimentMapper, "measurementDataMapper", measurementDataMapper);
    ReflectionTestUtils.setField(experimentMapper, "reactantMapper", reactantMapper);
    ReflectionTestUtils.setField(experimentMapper, "userMapper", userMapper);
    ReflectionTestUtils.setField(experimentMapper, "experimentAttachmentMapper", experimentAttachmentMapper);
  }

  // --- Shared fixture helpers, used across multiple @Nested groups below ---

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
      randomUUID(), randomUUID(), metadata, new MeasurementDataDto(1.2, 5.5, 0.1, false), List.of("10.1000/rxn123")
    );
  }

  private Experiment createMockEntityForSecurity(UUID experimentId, String creatorId, String scientistId, Instant archivedAt) {
    User creator = new User();
    creator.setId(creatorId);

    User scientist = new User();
    scientist.setId(scientistId);

    Metadata metadata = Metadata.builder().scientist(scientist).build();

    ExperimentResult result = ExperimentResult.builder().build();

    return Experiment.builder()
      .id(experimentId)
      .createdBy(creator)
      .metadata(metadata)
      .result(result)
      .archivedAt(archivedAt)
      .build();
  }

  /**
   * Stubs the repositories so that every reference contained in {@code dto} (scientist, both
   * reactants, solvent, instrument, modality, plus an ACTIVE structure for any reactant) resolves
   * successfully. Used by tests that need the "everything resolves" happy path, whether for
   * creating or updating an experiment.
   */
  private void mockReferencesResolvable(SaveExperimentDto dto) {
    User scientist = new User();
    scientist.setId(dto.metadata().scientist());
    when(userRepository.findById(dto.metadata().scientist())).thenReturn(Optional.of(scientist));

    Reactant reactantA = new Reactant();
    reactantA.setId(dto.reactantA());
    when(reactantRepository.findByIdForUpdate(dto.reactantA())).thenReturn(Optional.of(reactantA));

    Reactant reactantB = new Reactant();
    reactantB.setId(dto.reactantB());
    when(reactantRepository.findByIdForUpdate(dto.reactantB())).thenReturn(Optional.of(reactantB));

    ReactantStructure activeStructure = new ReactantStructure();
    activeStructure.setFlag(StructureFlag.ACTIVE);
    when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

    Solvent solvent = new Solvent();
    solvent.setId(dto.metadata().solvent());
    when(solventRepository.findById(dto.metadata().solvent())).thenReturn(Optional.of(solvent));

    Instrument instrument = new Instrument();
    instrument.setId(dto.metadata().instrument());
    when(instrumentRepository.findById(dto.metadata().instrument())).thenReturn(Optional.of(instrument));

    Modality modality = new Modality();
    modality.setId(dto.metadata().modality());
    when(modalityRepository.findById(dto.metadata().modality())).thenReturn(Optional.of(modality));
  }

  @Nested
  class CreateExperiment {

    @Test
    void createExperiment_whenValidInput_returnsGetExperimentDto() {
      String userId = "mock-keycloak-id-123";
      SaveExperimentDto validRequestDto = createDummyExperimentDto();

      User mockCreator = new User(); mockCreator.setId(userId);
      when(userRepository.findById(userId)).thenReturn(Optional.of(mockCreator));

      mockReferencesResolvable(validRequestDto);

      when(experimentRepository.save(any())).thenAnswer(invocation -> {
        Experiment saved = invocation.getArgument(0);
        saved.setId(randomUUID());
        return saved;
      });

      GetExperimentDto result = experimentService.createExperiment(validRequestDto, userId, Role.USER);

      ArgumentCaptor<Experiment> experimentCaptor = ArgumentCaptor.forClass(Experiment.class);
      verify(experimentRepository).save(experimentCaptor.capture());
      Experiment capturedEntity = experimentCaptor.getValue();

      assertNotNull(capturedEntity, "The captured entity should not be null");
      assertEquals(List.of("10.1000/rxn123"), capturedEntity.getDois());
      assertNotNull(result, "The returned DTO should not be null");
      assertEquals(List.of("10.1000/rxn123"), result.dois());
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

      verify(experimentRepository, never()).save(any());
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

      verify(experimentRepository, never()).save(any());
    }

    @Test
    void createExperiment_whenReactantIsMissing_throwsUnprocessableEntityException() {
      String validUserId = "mock-keycloak-id-123";
      SaveExperimentDto validDto = createDummyExperimentDto();

      // Reactant does not exist
      when(userRepository.findById(validUserId)).thenReturn(Optional.of(new User()));
      when(userRepository.findById(validDto.metadata().scientist())).thenReturn(Optional.of(new User()));
      when(reactantRepository.findByIdForUpdate(validDto.reactantA())).thenReturn(Optional.empty());

      UnprocessableEntityException exception = assertThrows(
        UnprocessableEntityException.class,
        () -> experimentService.createExperiment(validDto, validUserId, Role.USER)
      );

      assertTrue(exception.getMessage().contains("Reactant A does not exist"));
      verify(experimentRepository, never()).save(any());
    }
  }

  @Nested
  class GetExperiment {

    @Test
    void getExperiment_whenFound_returnsDto() {
      UUID experimentId = randomUUID();
      String requestingUserId = "user-123";

      Experiment mockExperiment = new Experiment();
      mockExperiment.setId(experimentId);
      mockExperiment.setDois(List.of("10.1000/rxn999"));

      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

      GetExperimentDto result = experimentService.getExperiment(experimentId, requestingUserId, Role.USER);

      assertNotNull(result, "The returned DTO should not be null");
      assertEquals(experimentId, result.uuid());
      assertEquals(List.of("10.1000/rxn999"), result.dois());
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
  }

  @Nested
  class ArchiveExperiment {

    @Test
    void archiveExperiment_whenUserIsCreator_archivesAndSaves() {
      UUID experimentId = randomUUID();
      String creatorId = "creator-123";

      Experiment mockExperiment = createMockEntityForSecurity(experimentId, creatorId, "scientist-456", null);
      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

      experimentService.archiveExperiment(experimentId, creatorId, Role.USER);

      assertNotNull(mockExperiment.getArchivedAt(), "Archived timestamp should be set");
      verify(experimentRepository).save(mockExperiment);
    }

    @Test
    void archiveExperiment_whenUserIsAdmin_archivesAndSaves() {
      UUID experimentId = randomUUID();

      Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", null);
      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

      experimentService.archiveExperiment(experimentId, "admin-999", Role.ADMIN);

      assertNotNull(mockExperiment.getArchivedAt());
      verify(experimentRepository).save(mockExperiment);
    }

    @Test
    void archiveExperiment_whenUserIsForbidden_throwsAccessDeniedException() {
      UUID experimentId = randomUUID();

      Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", null);
      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

      assertThrows(AccessDeniedException.class, () ->
        experimentService.archiveExperiment(experimentId, "snooper-000", Role.USER)
      );

      verify(experimentRepository, never()).save(any());
    }

    @Test
    void archiveExperiment_whenNotFound_throwsResourceNotFoundException() {
      UUID experimentId = randomUUID();
      String creatorId = "creator-123";

      when(experimentRepository.findById(experimentId)).thenReturn(Optional.empty());

      ResourceNotFoundException exception = assertThrows(
        ResourceNotFoundException.class,
        () -> experimentService.archiveExperiment(experimentId, creatorId, Role.USER)
      );

      assertTrue(exception.getMessage().contains("Experiment with ID " + experimentId + " not found"));
      verify(experimentRepository, never()).save(any());
    }

    @Test
    void archiveExperiment_whenAlreadyArchived_isNoOpAndDoesNotSave() {
      UUID experimentId = randomUUID();
      String creatorId = "creator-123";
      Instant originalArchivedAt = Instant.now();

      Experiment mockExperiment = createMockEntityForSecurity(experimentId, creatorId, "scientist-456", originalArchivedAt);
      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

      experimentService.archiveExperiment(experimentId, creatorId, Role.USER);

      assertEquals(originalArchivedAt, mockExperiment.getArchivedAt(), "Archived timestamp should remain unchanged");
      verify(experimentRepository, never()).save(any());
    }
  }

  @Nested
  class RestoreExperiment {

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
      verify(experimentRepository).save(mockExperiment);
    }

    @Test
    void restoreExperiment_whenUserIsForbidden_throwsAccessDeniedException() {
      UUID experimentId = randomUUID();

      Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", Instant.now());
      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

      assertThrows(AccessDeniedException.class, () ->
        experimentService.restoreExperiment(experimentId, "snooper-000", Role.USER)
      );

      verify(experimentRepository, never()).save(any());
    }

    @Test
    void restoreExperiment_whenNotFound_throwsResourceNotFoundException() {
      UUID experimentId = randomUUID();
      String scientistId = "scientist-456";

      when(experimentRepository.findById(experimentId)).thenReturn(Optional.empty());

      ResourceNotFoundException exception = assertThrows(
        ResourceNotFoundException.class,
        () -> experimentService.restoreExperiment(experimentId, scientistId, Role.USER)
      );

      assertTrue(exception.getMessage().contains("Experiment with ID " + experimentId + " not found"));
      verify(experimentRepository, never()).save(any());
    }

    @Test
    void restoreExperiment_whenAlreadyActive_isNoOpAndDoesNotSave() {
      UUID experimentId = randomUUID();
      String scientistId = "scientist-456";

      // Experiment is already active (not archived)
      Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", scientistId, null);
      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

      GetExperimentDto result = experimentService.restoreExperiment(experimentId, scientistId, Role.USER);

      Assertions.assertNull(mockExperiment.getArchivedAt(), "Archived timestamp should remain null");
      assertNotNull(result, "The returned DTO should still be built");
      verify(experimentRepository, never()).save(any());
    }
  }

  @Nested
  class GetArchivedExperiments {

    @Test
    void getArchivedExperiments_whenUserIsAdmin_fetchesAllArchived() {
      String adminId = "admin-999";

      // Create a couple of mock archived experiments
      Experiment exp1 = createMockEntityForSecurity(randomUUID(), "creator-1", "scientist-1", Instant.now());
      Experiment exp2 = createMockEntityForSecurity(randomUUID(), "creator-2", "scientist-2", Instant.now());

      when(experimentRepository.findByArchivedAtIsNotNull()).thenReturn(java.util.List.of(exp1, exp2));

      GetExperimentDto[] result = experimentService.getArchivedExperiments(adminId, Role.ADMIN);

      // Assert that the admin repository method was called
      verify(experimentRepository).findByArchivedAtIsNotNull();
      verify(experimentRepository, never()).findArchivedExperimentsForUser(any());

      // Assert array mapping
      assertNotNull(result, "Resulting array should not be null");
      assertEquals(2, result.length, "Should return exactly two archived experiments");
      assertEquals(exp1.getId(), result[0].uuid(), "First mapped DTO should match first entity ID");
      assertEquals(exp2.getId(), result[1].uuid(), "Second mapped DTO should match second entity ID");
    }

    @Test
    void getArchivedExperiments_whenUserIsStandardUser_fetchesUserSpecificArchived() {
      String userId = "standard-user-123";

      // Create a mock archived experiment belonging to this user
      Experiment exp1 = createMockEntityForSecurity(randomUUID(), userId, "some-scientist", Instant.now());

      when(experimentRepository.findArchivedExperimentsForUser(userId)).thenReturn(java.util.List.of(exp1));

      GetExperimentDto[] result = experimentService.getArchivedExperiments(userId, Role.USER);

      // Assert that the user-specific repository method was called
      verify(experimentRepository).findArchivedExperimentsForUser(userId);
      verify(experimentRepository, never()).findByArchivedAtIsNotNull();

      // Assert array mapping
      assertNotNull(result, "Resulting array should not be null");
      assertEquals(1, result.length, "Should return exactly one archived experiment");
      assertEquals(exp1.getId(), result[0].uuid());
    }

    @Test
    void getArchivedExperiments_whenEmptyResults_returnsEmptyArray() {
      String userId = "lonely-user-000";

      // Repository returns an empty list
      when(experimentRepository.findArchivedExperimentsForUser(userId)).thenReturn(java.util.Collections.emptyList());

      GetExperimentDto[] result = experimentService.getArchivedExperiments(userId, Role.USER);

      // Assert empty mapping is handled safely
      assertNotNull(result);
      assertEquals(0, result.length, "Array should be completely empty");
      verify(experimentRepository).findArchivedExperimentsForUser(userId);
    }
  }

  @Nested
  class UpdateExperiment {

    @Test
    void updateExperiment_whenValidAndAuthorized_updatesFieldsAndReturnsDto() {
      UUID experimentId = randomUUID();
      String creatorId = "creator-123";

      Experiment existingExperiment = createMockEntityForSecurity(experimentId, creatorId, "scientist-456", null);
      existingExperiment.setDois(List.of("10.1000/old-doi"));
      existingExperiment.getMetadata().setTemperature(20.0);

      SaveExperimentDto updateDto = createDummyExperimentDto();

      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(existingExperiment));
      mockReferencesResolvable(updateDto);
      when(experimentRepository.save(any(Experiment.class))).thenAnswer(i -> i.getArgument(0));

      GetExperimentDto result = experimentService.updateExperiment(experimentId, updateDto, creatorId, Role.USER);

      ArgumentCaptor<Experiment> experimentCaptor = ArgumentCaptor.forClass(Experiment.class);
      verify(experimentRepository).save(experimentCaptor.capture());
      Experiment savedExperiment = experimentCaptor.getValue();

      assertEquals(updateDto.dois(), savedExperiment.getDois(), "DOI should be updated");
      assertEquals(updateDto.metadata().temperature(), savedExperiment.getMetadata().getTemperature(), "Temperature should be updated");
      assertEquals(updateDto.metadata().scientist(), savedExperiment.getMetadata().getScientist().getId(), "Scientist should be updated");
      assertEquals(creatorId, savedExperiment.getCreatedBy().getId(), "Creator should NEVER change during an update");

      assertNotNull(result);
      assertEquals(updateDto.dois(), result.dois());
    }

    @Test
    void updateExperiment_ShouldFullyReplaceEveryDtoField_WhenUpdating() {
      // Regression test for full-replacement PUT semantics: every field present in
      // SaveExperimentDto/SaveMetadataDto/MeasurementDataDto must end up on the saved entity,
      // while server-managed fields not present in the DTO (createdBy, both archivedAt fields)
      // must remain untouched.
      UUID experimentId = randomUUID();
      String creatorId = "creator-123";

      Experiment existingExperiment = createMockEntityForSecurity(experimentId, creatorId, "scientist-456", null);
      existingExperiment.setDois(List.of("10.1000/old-doi"));
      existingExperiment.getMetadata().setTemperature(20.0);
      existingExperiment.getMetadata().setConcentrationA(0.1);
      existingExperiment.getMetadata().setConcentrationB(0.2);
      existingExperiment.getMetadata().setWavelength(100.0);
      existingExperiment.getMetadata().setComment("Old comment");
      existingExperiment.getResult().setRateConstant(0.001);
      existingExperiment.getResult().setPfoRateConstant(0.002);
      existingExperiment.getResult().setErrorValue(0.5);
      existingExperiment.getResult().setIsErrorPercentage(true);

      SaveExperimentDto updateDto = createDummyExperimentDto();

      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(existingExperiment));
      mockReferencesResolvable(updateDto);
      when(experimentRepository.save(any(Experiment.class))).thenAnswer(i -> i.getArgument(0));

      experimentService.updateExperiment(experimentId, updateDto, creatorId, Role.USER);

      ArgumentCaptor<Experiment> experimentCaptor = ArgumentCaptor.forClass(Experiment.class);
      verify(experimentRepository).save(experimentCaptor.capture());
      Experiment saved = experimentCaptor.getValue();

      // Top-level Experiment fields
      assertEquals(updateDto.dois(), saved.getDois(), "dois should be fully replaced");
      assertEquals(updateDto.reactantA(), saved.getReactantA().getId(), "reactantA should be fully replaced");
      assertEquals(updateDto.reactantB(), saved.getReactantB().getId(), "reactantB should be fully replaced");

      // Metadata fields
      assertEquals(updateDto.metadata().date(), saved.getMetadata().getDate(), "metadata.date should be fully replaced");
      assertEquals(updateDto.metadata().scientist(), saved.getMetadata().getScientist().getId(), "metadata.scientist should be fully replaced");
      assertEquals(updateDto.metadata().temperature(), saved.getMetadata().getTemperature(), "metadata.temperature should be fully replaced");
      assertEquals(updateDto.metadata().solvent(), saved.getMetadata().getSolvent().getId(), "metadata.solvent should be fully replaced");
      assertEquals(updateDto.metadata().concentrationA(), saved.getMetadata().getConcentrationA(), "metadata.concentrationA should be fully replaced");
      assertEquals(updateDto.metadata().concentrationB(), saved.getMetadata().getConcentrationB(), "metadata.concentrationB should be fully replaced");
      assertEquals(updateDto.metadata().instrument(), saved.getMetadata().getInstrument().getId(), "metadata.instrument should be fully replaced");
      assertEquals(updateDto.metadata().modality(), saved.getMetadata().getModality().getId(), "metadata.modality should be fully replaced");
      assertEquals(updateDto.metadata().wavelength(), saved.getMetadata().getWavelength(), "metadata.wavelength should be fully replaced");
      assertEquals(updateDto.metadata().comment(), saved.getMetadata().getComment(), "metadata.comment should be fully replaced");

      // ExperimentResult (measurementData) fields
      assertEquals(updateDto.measurementData().rateConstant(), saved.getResult().getRateConstant(), "result.rateConstant should be fully replaced");
      assertEquals(updateDto.measurementData().pFO_rateConstant(), saved.getResult().getPfoRateConstant(), "result.pfoRateConstant should be fully replaced");
      assertEquals(updateDto.measurementData().error(), saved.getResult().getErrorValue(), "result.errorValue should be fully replaced");
      assertEquals(updateDto.measurementData().isErrorPercentage(), saved.getResult().getIsErrorPercentage(), "result.isErrorPercentage should be fully replaced");

      // Server-managed fields NOT present in SaveExperimentDto must remain untouched by PUT
      assertEquals(creatorId, saved.getCreatedBy().getId(), "createdBy is not in the DTO and must never change during an update");
      Assertions.assertNull(saved.getArchivedAt(), "archivedAt is not in the DTO and must never be set by an update");
      Assertions.assertNull(saved.getMetadata().getArchivedAt(), "metadata.archivedAt is not in the DTO and must never be set by an update");
    }

    @Test
    void updateExperiment_whenUnauthorizedUser_throwsAccessDeniedException() {
      UUID experimentId = randomUUID();
      String snooperId = "snooper-000";

      Experiment existingExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", null);
      SaveExperimentDto updateDto = createDummyExperimentDto();

      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(existingExperiment));

      assertThrows(AccessDeniedException.class, () ->
        experimentService.updateExperiment(experimentId, updateDto, snooperId, Role.USER)
      );

      verify(experimentRepository, never()).save(any());
    }

    @Test
    void updateExperiment_whenArchived_throwsIllegalStateException() {
      UUID experimentId = randomUUID();
      String creatorId = "creator-123";

      // Experiment is archived! (archivedAt is not null)
      Experiment archivedExperiment = createMockEntityForSecurity(experimentId, creatorId, "scientist-456", Instant.now());
      SaveExperimentDto updateDto = createDummyExperimentDto();

      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(archivedExperiment));

      // We expect a ResourceConflictException because the entity is in a "frozen" state
      ResourceConflictException exception = assertThrows(ResourceConflictException.class, () ->
        experimentService.updateExperiment(experimentId, updateDto, creatorId, Role.USER)
      );

      assertTrue(exception.getMessage().toLowerCase().contains("archived"), "Exception message should mention the archived state");
      verify(experimentRepository, never()).save(any());
    }

    @Test
    void updateExperiment_whenArchivedAndUnauthorizedUser_throwsAccessDeniedException() {
      UUID experimentId = randomUUID();
      String snooperId = "snooper-000";

      // Experiment is archived AND the requester is neither creator, scientist, nor privileged.
      Experiment archivedExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", Instant.now());
      SaveExperimentDto updateDto = createDummyExperimentDto();

      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(archivedExperiment));

      // Authorization must be checked before the archived-state check, so an unauthorized caller
      // gets a 403 (AccessDeniedException) instead of a 409 (ResourceConflictException), without revealing
      // the experiment's archived status to someone with no view/modify rights over it.
      assertThrows(AccessDeniedException.class, () ->
        experimentService.updateExperiment(experimentId, updateDto, snooperId, Role.USER)
      );

      verify(experimentRepository, never()).save(any());
    }

    @Test
    void updateExperiment_whenReactantNotFound_throwsUnprocessableEntityException() {
      UUID experimentId = randomUUID();
      String creatorId = "creator-123";

      // 1. Existing entity in the DB
      Experiment existingExperiment = createMockEntityForSecurity(experimentId, creatorId, "scientist-456", null);
      SaveExperimentDto updateDto = createDummyExperimentDto();

      // 2. Setup mocks
      when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(existingExperiment));

      // Mock the scientist lookup so it passes and reaches the reactant lookup
      User newScientist = new User();
      newScientist.setId(updateDto.metadata().scientist());
      when(userRepository.findById(updateDto.metadata().scientist())).thenReturn(Optional.of(newScientist));

      // Mock Reactant A returning empty (simulating it does not exist in the DB)
      when(reactantRepository.findByIdForUpdate(updateDto.reactantA())).thenReturn(Optional.empty());

      // 3. Execute and Assert
      UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, () ->
        experimentService.updateExperiment(experimentId, updateDto, creatorId, Role.USER)
      );

      assertTrue(exception.getMessage().contains("Reactant A does not exist"), "Exception should specify which entity is missing");

      // Ensure we never attempted to save the corrupted state
      verify(experimentRepository, never()).save(any());
    }
  }

  @Nested
  class SearchExperiments {

    @Test
    void searchExperiments_executesPaginatedQueryAndReturnsMappedDtos() {
      // 1. Setup Request Data
      SearchParametersDto searchDto = new SearchParametersDto(
        List.of(randomUUID()), // reactant A
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
      );
      Pageable pageable = PageRequest.of(0, 10);
      String userId = "user-123";

      // 2. Setup Mock Database Response
      Experiment mockExperiment = createMockEntityForSecurity(randomUUID(), userId, "scientist-1", null);
      Page<Experiment> mockEntityPage =
        new PageImpl<>(List.of(mockExperiment));

      // When the repository executes ANY Specification with ANY Pageable, return our mock page.
      // Uses the explicitly-typed any() (via ArgumentMatchers.<Specification<Experiment>>any()) instead of
      // any(Specification.class), which required an unchecked conversion from the raw Specification type. A plain
      // parameterless any() doesn't compile here because ExperimentRepository has more than one overloaded
      // findAll(..., Pageable), making the target type ambiguous without an explicit type witness.
      when(experimentRepository.findAll(ArgumentMatchers.<Specification<Experiment>>any(), any(Pageable.class)))
        .thenReturn(mockEntityPage);

      // 3. Execute
      Page<GetExperimentDto> resultPage =
        experimentService.searchExperiments(searchDto, pageable, userId, Role.USER);

      // 4. Assert
      assertNotNull(resultPage, "Page should not be null");
      assertEquals(1, resultPage.getTotalElements(), "Page should contain exactly 1 mapped DTO");

      // Verify the repository was actually called with a Specification and the Pageable
      verify(experimentRepository).findAll(ArgumentMatchers.<Specification<Experiment>>any(), Mockito.eq(pageable));
    }
  }
}
