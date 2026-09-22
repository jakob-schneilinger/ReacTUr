package com.reactur.backend.services;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.dtos.folders.SaveFolderDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentFolder;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import com.reactur.backend.mappers.FolderMapper;
import com.reactur.backend.repositories.ExperimentFolderRepository;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import com.reactur.backend.services.implementations.ExperimentFolderServiceImpl;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/**
 * Pure unit test for {@link ExperimentFolderServiceImpl}, with repositories mocked. Covers folder
 * ownership enforcement (no privileged bypass), the one-folder-per-user rule when adding an
 * experiment, and that adding requires the experiment itself to be owned by the requesting user.
 */
class ExperimentFolderServiceTest {

  @Mock
  private ExperimentFolderRepository folderRepository;
  @Mock
  private ExperimentRepository experimentRepository;
  @Mock
  private UserRepository userRepository;

  @Spy
  private FolderMapper folderMapper = Mappers.getMapper(FolderMapper.class);

  @Spy
  private ExperimentAuthorization experimentAuthorization = new ExperimentAuthorization();

  @InjectMocks
  private ExperimentFolderServiceImpl folderService;

  private User creatorUser;

  private User scientistUser;
  private String scientistUserId;

  private Experiment sharedExperiment;
  private UUID sharedExperimentId;

  @BeforeEach
  void setUp() {
    creatorUser = new User();
    creatorUser.setId("creator-123");

    scientistUser = new User();
    scientistUserId = "scientist-456";
    scientistUser.setId(scientistUserId);

    sharedExperimentId = UUID.randomUUID();
    sharedExperiment = Experiment.builder()
      .id(sharedExperimentId)
      .createdBy(creatorUser)
      .metadata(Metadata.builder().scientist(scientistUser).build())
      .build();
  }

  @Test
  void addExperimentToFolder_whenExperimentInAnotherFolder_movesExperimentToNewFolder() {
    UUID oldFolderId = UUID.randomUUID();
    UUID newFolderId = UUID.randomUUID();

    // Old folder contains the experiment
    List<Experiment> oldFolderExperiments = new ArrayList<>();
    oldFolderExperiments.add(sharedExperiment);
    ExperimentFolder oldFolder = ExperimentFolder.builder().id(oldFolderId).name("Old Folder").owner(creatorUser).experiments(oldFolderExperiments).build();

    ExperimentFolder newFolder = ExperimentFolder.builder().id(newFolderId).name("New Folder").owner(creatorUser).experiments(new ArrayList<>()).build();

    when(folderRepository.findById(newFolderId)).thenReturn(Optional.of(newFolder));
    when(folderRepository.findAllByOwner_Id(creatorUser.getId())).thenReturn(List.of(oldFolder, newFolder));
    when(experimentRepository.findByIdForUpdate(sharedExperiment.getId())).thenReturn(Optional.of(sharedExperiment));

    GetFolderDto result = folderService.addExperimentToFolder(newFolderId, sharedExperiment.getId(), creatorUser.getId());

    assertNotNull(result);
    assertTrue(result.experimentIds().contains(sharedExperiment.getId()), "New folder should now contain the experiment");

    assertFalse(oldFolder.getExperiments().contains(sharedExperiment), "Experiment should be removed from the old folder");
    Mockito.verify(folderRepository).save(oldFolder);
    Mockito.verify(folderRepository).save(newFolder);
  }

  @Test
  void addExperimentToFolder_whenExperimentNotOwned_throwsAccessDeniedException() {
    UUID folderId = UUID.randomUUID();

    User snooperUser = new User();
    String snooperUserId = "snooper-123";
    snooperUser.setId(snooperUserId);

    List<Experiment> folderExperiments = new ArrayList<>();
    ExperimentFolder folder = ExperimentFolder.builder().id(folderId).name("Folder").owner(snooperUser).experiments(folderExperiments).build();

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
    when(experimentRepository.findByIdForUpdate(sharedExperiment.getId())).thenReturn(Optional.of(sharedExperiment));

    assertThrows(
      AccessDeniedException.class,
      () -> folderService.addExperimentToFolder(folderId, sharedExperimentId, snooperUserId)
    );

    Mockito.verify(folderRepository, Mockito.never()).save(any(ExperimentFolder.class));
  }

  @Test
  void addExperimentToFolder_whenFolderNotOwned_throwsAccessDeniedException() {
    UUID folderId = UUID.randomUUID();

    User snooperUser = new User();
    String snooperUserId = "snooper-123";
    snooperUser.setId(snooperUserId);

    List<Experiment> folderExperiments = new ArrayList<>();
    folderExperiments.add(sharedExperiment);
    ExperimentFolder folder = ExperimentFolder.builder().id(folderId).name("Folder").owner(scientistUser).experiments(folderExperiments).build();

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

    assertThrows(
      AccessDeniedException.class,
      () -> folderService.addExperimentToFolder(folderId, sharedExperimentId, snooperUserId)
    );

    Mockito.verify(folderRepository, Mockito.never()).save(any(ExperimentFolder.class));
  }

  // Creator and Scientist can have the same exp in different folders
  @Test
  void addExperimentToFolder_whenDifferentUsersAddSameExperiment_doesNotInterfere() {
    // Setup Creator's Folder
    UUID creatorFolderId = UUID.randomUUID();
    ExperimentFolder creatorFolder = ExperimentFolder.builder().id(creatorFolderId).name("Creator Data").owner(creatorUser).experiments(new ArrayList<>(List.of(sharedExperiment))).build();

    // Setup Scientist's Folder (Empty initially)
    UUID scientistFolderId = UUID.randomUUID();
    ExperimentFolder scientistFolder = ExperimentFolder.builder().id(scientistFolderId).name("Scientist Data").owner(scientistUser).experiments(new ArrayList<>()).build();

    // Mock Scientist adding the experiment to their folder
    when(folderRepository.findById(scientistFolderId)).thenReturn(Optional.of(scientistFolder));

    // The repository only returns the SCIENTIST'S folders.
    // It never sees the Creator's folder, so it cannot accidentally remove the experiment from it!
    when(folderRepository.findAllByOwner_Id(scientistUser.getId())).thenReturn(List.of(scientistFolder));
    when(experimentRepository.findByIdForUpdate(sharedExperiment.getId())).thenReturn(Optional.of(sharedExperiment));

    GetFolderDto result = folderService.addExperimentToFolder(scientistFolderId, sharedExperiment.getId(), scientistUser.getId());

    assertTrue(result.experimentIds().contains(sharedExperiment.getId()), "Scientist folder should contain the experiment");

    // Verify the creator's folder was completely untouched (save was never called on it)
    assertTrue(creatorFolder.getExperiments().contains(sharedExperiment), "Creator's folder MUST still contain the experiment");
    Mockito.verify(folderRepository, Mockito.never()).save(creatorFolder);
  }

  @Test
  void createFolder_whenValidInput_returnsGetFolderDto() {
    SaveFolderDto inputDto = new SaveFolderDto("My Synthesis");
    when(userRepository.findById(creatorUser.getId())).thenReturn(Optional.of(creatorUser));
    when(folderRepository.save(any(ExperimentFolder.class))).thenAnswer(i -> {
      ExperimentFolder f = i.getArgument(0);
      f.setId(UUID.randomUUID());
      return f;
    });

    GetFolderDto result = folderService.createFolder(inputDto, creatorUser.getId());

    assertNotNull(result.id());
    assertEquals("My Synthesis", result.name());
    assertTrue(result.experimentIds().isEmpty(), "New folder should be empty");
  }

  @Test
  void createFolder_whenUserNotFound_throwsIllegalStateException() {
    SaveFolderDto inputDto = new SaveFolderDto("My Synthesis");
    when(userRepository.findById(creatorUser.getId())).thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class, () -> folderService.createFolder(inputDto, creatorUser.getId()));

    Mockito.verify(folderRepository, Mockito.never()).save(any());
  }

  @Test
  void getMyFolders_returnsOnlyOwnedFolders() {
    ExperimentFolder f1 = ExperimentFolder.builder().id(UUID.randomUUID()).name("Folder 1").owner(creatorUser).experiments(new ArrayList<>()).build();
    ExperimentFolder f2 = ExperimentFolder.builder().id(UUID.randomUUID()).name("Folder 2").owner(creatorUser).experiments(new ArrayList<>()).build();

    when(folderRepository.findAllByOwner_Id(creatorUser.getId())).thenReturn(List.of(f1, f2));

    List<GetFolderDto> results = folderService.getMyFolders(creatorUser.getId());

    assertEquals(2, results.size());
    Mockito.verify(folderRepository).findAllByOwner_Id(creatorUser.getId());
  }

  @Test
  void addExperimentToFolder_whenUserDoesNotOwnFolder_throwsAccessDeniedException() {
    UUID folderId = UUID.randomUUID();
    ExperimentFolder creatorFolder = ExperimentFolder.builder().id(folderId).name("Creator Data").owner(creatorUser).experiments(new ArrayList<>()).build();

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(creatorFolder));

    // Scientist tries to add an experiment to Creator's folder
    assertThrows(AccessDeniedException.class, () ->
      folderService.addExperimentToFolder(folderId, sharedExperimentId, scientistUserId)
    );

    Mockito.verify(folderRepository, Mockito.never()).save(any());
  }

  @Test
  void addExperimentToFolder_whenExperimentNotFound_throwsResourceNotFoundException() {
    UUID folderId = UUID.randomUUID();
    UUID missingExperimentId = UUID.randomUUID();
    ExperimentFolder folder = ExperimentFolder.builder().id(folderId).name("My Data").owner(creatorUser).experiments(new ArrayList<>()).build();

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
    when(experimentRepository.findByIdForUpdate(missingExperimentId)).thenReturn(Optional.empty());

    assertThrows(
      ResourceNotFoundException.class,
      () -> folderService.addExperimentToFolder(folderId, missingExperimentId, creatorUser.getId())
    );

    Mockito.verify(folderRepository, Mockito.never()).save(any(ExperimentFolder.class));
  }

  @Test
  void removeExperimentFromFolder_removesAndSaves() {
    UUID folderId = UUID.randomUUID();
    List<Experiment> exps = new ArrayList<>();
    exps.add(sharedExperiment);
    ExperimentFolder folder = ExperimentFolder.builder().id(folderId).name("My Data").owner(creatorUser).experiments(exps).build();

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

    GetFolderDto result = folderService.removeExperimentFromFolder(folderId, sharedExperiment.getId(), creatorUser.getId());

    assertTrue(result.experimentIds().isEmpty(), "Experiment should be removed");
    Mockito.verify(folderRepository).save(folder);
  }

  @Test
  void removeExperimentFromFolder_whenFolderNotFound_throwsResourceNotFoundException() {
    UUID folderId = UUID.randomUUID();
    when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

    assertThrows(
      ResourceNotFoundException.class,
      () -> folderService.removeExperimentFromFolder(folderId, sharedExperimentId, creatorUser.getId())
    );

    Mockito.verify(folderRepository, Mockito.never()).save(any());
  }

  @Test
  void deleteFolder_whenAuthorized_deletesFolder() {
    UUID folderId = UUID.randomUUID();
    ExperimentFolder folder = ExperimentFolder.builder().id(folderId).name("To Delete").owner(creatorUser).experiments(new ArrayList<>()).build();

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

    folderService.deleteFolder(folderId, creatorUser.getId());

    ArgumentCaptor<ExperimentFolder> folderCaptor = ArgumentCaptor.forClass(ExperimentFolder.class);
    Mockito.verify(folderRepository).delete(folderCaptor.capture());
    assertEquals(folderId, folderCaptor.getValue().getId());
  }

  @Test
  void deleteFolder_whenFolderNotOwned_throwsAccessDeniedException() {
    UUID folderId = UUID.randomUUID();
    ExperimentFolder folder = ExperimentFolder.builder().id(folderId).name("Creator Data").owner(creatorUser).experiments(new ArrayList<>()).build();

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

    assertThrows(
      AccessDeniedException.class,
      () -> folderService.deleteFolder(folderId, scientistUserId)
    );

    Mockito.verify(folderRepository, Mockito.never()).delete(any());
  }
}
