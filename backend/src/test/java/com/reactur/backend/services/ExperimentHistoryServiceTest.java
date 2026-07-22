package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.ResourceNotFoundException;
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
