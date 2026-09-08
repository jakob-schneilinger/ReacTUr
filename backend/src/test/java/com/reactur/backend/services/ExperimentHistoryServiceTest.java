package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import com.reactur.backend.services.implementations.ExperimentHistoryServiceImpl;
import org.javers.common.string.PrettyValuePrinter;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentHistoryServiceTest {

  @Mock private Javers javers;
  @Mock private ExperimentRepository experimentRepository;
  @Mock private InstrumentRepository instrumentRepository;
  @Mock private ModalityRepository modalityRepository;
  @Mock private SolventRepository solventRepository;
  @Mock private ReactantRepository reactantRepository;
  @Mock private UserRepository userRepository;

  @Spy private ExperimentAuthorization experimentAuthorization = new ExperimentAuthorization();

  @InjectMocks
  private ExperimentHistoryServiceImpl experimentHistoryService;

  @Test
  void getExperimentHistory_whenNotFound_throwsResourceNotFoundException() {
    UUID experimentId = randomUUID();

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () ->
      experimentHistoryService.getExperimentHistory(experimentId, "user-123", Role.USER)
    );
  }

  @Test
  void getExperimentHistory_whenActiveAndUnrelatedApprovedUser_returnsHistory() {
    UUID experimentId = randomUUID();
    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", null);

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));
    when(javers.findChanges(any())).thenReturn(emptyChanges());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "unrelated-user-999", Role.USER);

    assertNotNull(result, "An unrelated approved user should be able to view an active experiment's history");
    assertTrue(result.isEmpty());
    // The experiment must only be fetched once and reused for the authorization check.
    verify(experimentRepository, times(1)).findById(experimentId);
  }

  @Test
  void getExperimentHistory_whenArchivedAndUserIsCreator_returnsHistory() {
    UUID experimentId = randomUUID();
    String creatorId = "creator-123";
    Experiment mockExperiment = createMockEntityForSecurity(experimentId, creatorId, "scientist-456", Instant.now());

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));
    when(javers.findChanges(any())).thenReturn(emptyChanges());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, creatorId, Role.USER);

    assertNotNull(result, "Creator should be able to view their archived experiment's history");
  }

  @Test
  void getExperimentHistory_whenArchivedAndUserIsScientist_returnsHistory() {
    UUID experimentId = randomUUID();
    String scientistId = "scientist-456";
    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", scientistId, Instant.now());

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));
    when(javers.findChanges(any())).thenReturn(emptyChanges());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, scientistId, Role.USER);

    assertNotNull(result, "Scientist should be able to view their archived experiment's history");
  }

  @Test
  void getExperimentHistory_whenArchivedAndUserIsAdmin_returnsHistory() {
    UUID experimentId = randomUUID();
    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", Instant.now());

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));
    when(javers.findChanges(any())).thenReturn(emptyChanges());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "admin-999", Role.ADMIN);

    assertNotNull(result, "Admins should be able to view any archived experiment's history");
  }

  @Test
  void getExperimentHistory_whenArchivedAndUserIsSupervisor_returnsHistory() {
    UUID experimentId = randomUUID();
    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", Instant.now());

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));
    when(javers.findChanges(any())).thenReturn(emptyChanges());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "supervisor-999", Role.SUPERVISOR);

    assertNotNull(result, "Supervisors should be able to view any archived experiment's history");
  }

  @Test
  void getExperimentHistory_whenArchivedAndUserIsForbidden_throwsAccessDeniedException() {
    UUID experimentId = randomUUID();
    Experiment mockExperiment = createMockEntityForSecurity(experimentId, "creator-123", "scientist-456", Instant.now());

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(mockExperiment));

    // Standard user with no relation to the experiment
    assertThrows(AccessDeniedException.class, () ->
      experimentHistoryService.getExperimentHistory(experimentId, "snooper-000", Role.USER)
    );
  }

  // ---------------------------------------------------------------------
  // Diff/history-building logic tests.
  //
  // The tests above all stub javers.findChanges(any()) to return an empty
  // list, which only exercises the authorization branches. The tests below
  // instead wire the mocked `javers` field to delegate to a real, in-memory
  // Javers instance (JaversBuilder.javers().build()) that has had real
  // entities committed to it, so the genuine Javers Change subtypes
  // (NewObject/ValueChange/ReferenceChange) returned by findChanges drive
  // the production code's CREATE/UPDATE/ARCHIVE/RESTORE detection and
  // diff-building logic. This was chosen over hand-constructing fake Change
  // objects because Javers' Change subtypes have non-trivial constructors
  // and internal invariants that would be fragile to fabricate by hand;
  // exercising the real API end-to-end is more reliable.
  // ---------------------------------------------------------------------

  @Test
  void getExperimentHistory_withGenuineFirstCommit_producesCreateCommitType() {
    UUID experimentId = randomUUID();
    Metadata metadata = Metadata.builder().id(randomUUID()).temperature(25.0).build();
    Experiment experiment = experimentWithMetadata(experimentId, metadata);

    Javers realJavers = JaversBuilder.javers().build();
    realJavers.commit("author-1", metadata);

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
    delegateToRealJavers(realJavers);
    when(userRepository.findAllById(any())).thenReturn(List.of());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "any-user", Role.USER);

    assertEquals(1, result.size());
    assertEquals("CREATE", result.get(0).commitType());
  }

  @Test
  void getExperimentHistory_withGenuineValueChange_producesUpdateEntryWithOldAndNewValue() {
    UUID experimentId = randomUUID();
    UUID metadataId = randomUUID();
    Metadata metadataV1 = Metadata.builder().id(metadataId).comment("Old comment").build();
    Metadata metadataV2 = Metadata.builder().id(metadataId).comment("New comment").build();
    Experiment experiment = experimentWithMetadata(experimentId, metadataV2);

    Javers realJavers = JaversBuilder.javers().build();
    realJavers.commit("author-1", metadataV1);
    realJavers.commit("author-1", metadataV2);

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
    delegateToRealJavers(realJavers);
    when(userRepository.findAllById(any())).thenReturn(List.of());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "any-user", Role.USER);

    assertEquals(2, result.size());
    ExperimentHistoryDto updateEntry = result.stream()
      .filter(dto -> "UPDATE".equals(dto.commitType()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Expected an UPDATE entry"));

    assertEquals("Old comment -> New comment", updateEntry.changes().get("comment"));
  }

  @Test
  void getExperimentHistory_withGenuineReferenceChange_resolvesEntityNamesInsteadOfRawIds() {
    UUID experimentId = randomUUID();
    UUID metadataId = randomUUID();
    Solvent solventA = Solvent.builder().id(randomUUID()).name("Ethanol").build();
    Solvent solventB = Solvent.builder().id(randomUUID()).name("Methanol").build();
    Metadata metadataV1 = Metadata.builder().id(metadataId).solvent(solventA).build();
    Metadata metadataV2 = Metadata.builder().id(metadataId).solvent(solventB).build();
    Experiment experiment = experimentWithMetadata(experimentId, metadataV2);

    Javers realJavers = JaversBuilder.javers().build();
    realJavers.commit("author-1", metadataV1);
    realJavers.commit("author-1", metadataV2);

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
    delegateToRealJavers(realJavers);
    when(userRepository.findAllById(any())).thenReturn(List.of());
    when(solventRepository.findAllById(any())).thenReturn(List.of(solventA, solventB));

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "any-user", Role.USER);

    ExperimentHistoryDto updateEntry = result.stream()
      .filter(dto -> "UPDATE".equals(dto.commitType()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Expected an UPDATE entry"));

    // Resolved via buildEntityLookupMap/extractId: human-readable names, not raw UUIDs.
    assertEquals("Ethanol -> Methanol", updateEntry.changes().get("solvent"));
  }

  @Test
  void getExperimentHistory_withGenuineListChange_includesDoisChangeInDiffMap() {
    UUID experimentId = randomUUID();
    User creator = User.builder().id("creator-1").build();

    Experiment experimentV1 = Experiment.builder()
      .id(experimentId)
      .createdBy(creator)
      .dois(List.of("10.1000/old-doi"))
      .build();
    Experiment experimentV2 = Experiment.builder()
      .id(experimentId)
      .createdBy(creator)
      .dois(List.of("10.1000/new-doi"))
      .build();

    Javers realJavers = JaversBuilder.javers().build();
    realJavers.commit("author-1", experimentV1);
    realJavers.commit("author-1", experimentV2);

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experimentV2));
    delegateToRealJavers(realJavers);
    when(userRepository.findAllById(any())).thenReturn(List.of());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "any-user", Role.USER);

    ExperimentHistoryDto updateEntry = result.stream()
      .filter(dto -> "UPDATE".equals(dto.commitType()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Expected an UPDATE entry"));

    // Regression test: a previous version of ExperimentHistoryServiceImpl silently dropped
    // list-typed property changes (Javers' ListChange) from the diff map entirely, because its
    // instanceof-chain only handled ValueChange/ReferenceChange explicitly. Confirmed empirically
    // before the rewrite - the changes map came back completely empty for this exact scenario.
    assertTrue(updateEntry.changes().containsKey("dois"), "Expected 'dois' to appear in the diff map");
    assertEquals("[10.1000/old-doi] -> [10.1000/new-doi]", updateEntry.changes().get("dois"));
  }

  @Test
  void getExperimentHistory_whenArchivedAtGoesFromNullToSet_producesArchiveCommitTypeAndOmitsProperty() {
    UUID experimentId = randomUUID();
    UUID metadataId = randomUUID();
    Metadata metadataV1 = Metadata.builder().id(metadataId).archivedAt(null).build();
    Metadata metadataV2 = Metadata.builder().id(metadataId).archivedAt(Instant.now()).build();
    Experiment experiment = experimentWithMetadata(experimentId, metadataV2);

    Javers realJavers = JaversBuilder.javers().build();
    realJavers.commit("author-1", metadataV1);
    realJavers.commit("author-1", metadataV2);

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
    delegateToRealJavers(realJavers);
    when(userRepository.findAllById(any())).thenReturn(List.of());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "any-user", Role.USER);

    ExperimentHistoryDto archiveEntry = result.stream()
      .filter(dto -> "ARCHIVE".equals(dto.commitType()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Expected an ARCHIVE entry"));

    // archivedAt is an IGNORED_PROPERTIES entry: it drives the commit type but must not appear in the diff map.
    assertFalse(archiveEntry.changes().containsKey("archivedAt"));
  }

  @Test
  void getExperimentHistory_whenArchivedAtGoesFromSetToNull_producesRestoreCommitType() {
    UUID experimentId = randomUUID();
    UUID metadataId = randomUUID();
    Metadata metadataV1 = Metadata.builder().id(metadataId).archivedAt(Instant.now()).build();
    Metadata metadataV2 = Metadata.builder().id(metadataId).archivedAt(null).build();
    Experiment experiment = experimentWithMetadata(experimentId, metadataV2);

    Javers realJavers = JaversBuilder.javers().build();
    realJavers.commit("author-1", metadataV1);
    realJavers.commit("author-1", metadataV2);

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
    delegateToRealJavers(realJavers);
    when(userRepository.findAllById(any())).thenReturn(List.of());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "any-user", Role.USER);

    boolean hasRestoreEntry = result.stream().anyMatch(dto -> "RESTORE".equals(dto.commitType()));
    assertTrue(hasRestoreEntry, "Expected a RESTORE commit type when archivedAt transitions from set to null");
  }

  @Test
  void getExperimentHistory_withIgnoredNonArchivedAtProperty_isExcludedFromDiffMap() {
    UUID experimentId = randomUUID();
    User creator = User.builder().id("creator-1").build();
    ExperimentResult resultV1 = ExperimentResult.builder()
      .id(randomUUID()).rateConstant(1.0).errorValue(0.1).isErrorPercentage(false).build();
    ExperimentResult resultV2 = ExperimentResult.builder()
      .id(randomUUID()).rateConstant(2.0).errorValue(0.2).isErrorPercentage(false).build();

    Experiment experimentV1 = Experiment.builder()
      .id(experimentId).createdBy(creator).result(resultV1).build();
    Experiment experimentV2 = Experiment.builder()
      .id(experimentId).createdBy(creator).result(resultV2).build();

    Javers realJavers = JaversBuilder.javers().build();
    realJavers.commit("author-1", experimentV1);
    realJavers.commit("author-1", experimentV2);

    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experimentV2));
    delegateToRealJavers(realJavers);
    when(userRepository.findAllById(any())).thenReturn(List.of());

    List<ExperimentHistoryDto> result =
      experimentHistoryService.getExperimentHistory(experimentId, "creator-1", Role.USER);

    boolean anyChangeMapContainsResultKey = result.stream()
      .flatMap(dto -> dto.changes().keySet().stream())
      .anyMatch(key -> key.equals("result"));

    assertFalse(anyChangeMapContainsResultKey,
      "\"result\" is an IGNORED_PROPERTIES entry and must never appear in the diff map");
  }

  private void delegateToRealJavers(Javers realJavers) {
    when(javers.findChanges(any())).thenAnswer(invocation -> realJavers.findChanges(invocation.getArgument(0)));
  }

  private Experiment experimentWithMetadata(UUID experimentId, Metadata metadata) {
    User creator = User.builder().id("creator-1").build();
    return Experiment.builder()
      .id(experimentId)
      .createdBy(creator)
      .metadata(metadata)
      .build();
  }

  private Changes emptyChanges() {
    return new Changes(List.of(), PrettyValuePrinter.getDefault());
  }

  private Experiment createMockEntityForSecurity(UUID experimentId, String creatorId, String scientistId, Instant archivedAt) {
    User creator = new User();
    creator.setId(creatorId);

    User scientist = new User();
    scientist.setId(scientistId);

    Metadata metadata = Metadata.builder().id(randomUUID()).scientist(scientist).build();
    ExperimentResult result = ExperimentResult.builder().id(randomUUID()).build();

    return Experiment.builder()
      .id(experimentId)
      .createdBy(creator)
      .metadata(metadata)
      .result(result)
      .archivedAt(archivedAt)
      .build();
  }
}
