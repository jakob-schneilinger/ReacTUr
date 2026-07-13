package com.reactur.backend.services;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.dtos.folders.SaveFolderDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentFolder;
import com.reactur.backend.mappers.FolderMapper;
import com.reactur.backend.repositories.ExperimentFolderRepository;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.UserRepository;
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
class ExperimentFolderServiceTest {

  @Mock
  private ExperimentFolderRepository folderRepository;
  @Mock
  private ExperimentRepository experimentRepository;
  @Mock
  private UserRepository userRepository;

  @Spy
  private FolderMapper folderMapper = Mappers.getMapper(FolderMapper.class);

  @InjectMocks
  private ExperimentFolderServiceImpl folderService;

  private User creatorUser;
  private User scientistUser;
  private Experiment sharedExperiment;

  @BeforeEach
  void setUp() {
    creatorUser = new User();
    creatorUser.setId("creator-123");

    scientistUser = new User();
    scientistUser.setId("scientist-456");

    sharedExperiment = Experiment.builder()
      .id(UUID.randomUUID())
      .build();
  }

  @Test
  void addExperimentToFolder_whenExperimentInAnotherFolder_movesExperimentToNewFolder() {
    UUID oldFolderId = UUID.randomUUID();
    UUID newFolderId = UUID.randomUUID();

    // Old folder contains the experiment
    List<Experiment> oldFolderExperiments = new ArrayList<>();
    oldFolderExperiments.add(sharedExperiment);
    ExperimentFolder oldFolder = new ExperimentFolder(oldFolderId, "Old Folder", creatorUser, oldFolderExperiments);

    ExperimentFolder newFolder = new ExperimentFolder(newFolderId, "New Folder", creatorUser, new ArrayList<>());

    when(folderRepository.findById(newFolderId)).thenReturn(Optional.of(newFolder));
    when(folderRepository.findAllByOwner_Id(creatorUser.getId())).thenReturn(List.of(oldFolder, newFolder));
    when(experimentRepository.findById(sharedExperiment.getId())).thenReturn(Optional.of(sharedExperiment));

    GetFolderDto result = folderService.addExperimentToFolder(newFolderId, sharedExperiment.getId(), creatorUser.getId());

    assertNotNull(result);
    assertTrue(result.experimentIds().contains(sharedExperiment.getId()), "New folder should now contain the experiment");

    assertFalse(oldFolder.getExperiments().contains(sharedExperiment), "Experiment should be removed from the old folder");
    Mockito.verify(folderRepository).save(oldFolder);
    Mockito.verify(folderRepository).save(newFolder);
  }

  // Creator and Scientist can have the same exp in different folders
  @Test
  void addExperimentToFolder_whenDifferentUsersAddSameExperiment_doesNotInterfere() {
    // Setup Creator's Folder
    UUID creatorFolderId = UUID.randomUUID();
    ExperimentFolder creatorFolder = new ExperimentFolder(creatorFolderId, "Creator Data", creatorUser, new ArrayList<>(List.of(sharedExperiment)));

    // Setup Scientist's Folder (Empty initially)
    UUID scientistFolderId = UUID.randomUUID();
    ExperimentFolder scientistFolder = new ExperimentFolder(scientistFolderId, "Scientist Data", scientistUser, new ArrayList<>());

    // Mock Scientist adding the experiment to their folder
    when(folderRepository.findById(scientistFolderId)).thenReturn(Optional.of(scientistFolder));

    // The repository only returns the SCIENTIST'S folders.
    // It never sees the Creator's folder, so it cannot accidentally remove the experiment from it!
    when(folderRepository.findAllByOwner_Id(scientistUser.getId())).thenReturn(List.of(scientistFolder));
    when(experimentRepository.findById(sharedExperiment.getId())).thenReturn(Optional.of(sharedExperiment));

    GetFolderDto result = folderService.addExperimentToFolder(scientistFolderId, sharedExperiment.getId(), scientistUser.getId());

    assertTrue(result.experimentIds().contains(sharedExperiment.getId()), "Scientist folder should contain the experiment");

    // Verify the creator's folder was completely untouched (save was never called on it)
    assertTrue(creatorFolder.getExperiments().contains(sharedExperiment), "Creator's folder MUST still contain the experiment");
    Mockito.verify(folderRepository, Mockito.never()).save(creatorFolder);
  }

  // ===================================================================================
  // STANDARD CRUD VALIDATIONS
  // ===================================================================================
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
  void getMyFolders_returnsOnlyOwnedFolders() {
    ExperimentFolder f1 = new ExperimentFolder(UUID.randomUUID(), "Folder 1", creatorUser, new ArrayList<>());
    ExperimentFolder f2 = new ExperimentFolder(UUID.randomUUID(), "Folder 2", creatorUser, new ArrayList<>());

    when(folderRepository.findAllByOwner_Id(creatorUser.getId())).thenReturn(List.of(f1, f2));

    List<GetFolderDto> results = folderService.getMyFolders(creatorUser.getId());

    assertEquals(2, results.size());
    Mockito.verify(folderRepository).findAllByOwner_Id(creatorUser.getId());
  }

  @Test
  void addExperimentToFolder_whenUserDoesNotOwnFolder_throwsAccessDeniedException() {
    UUID folderId = UUID.randomUUID();
    ExperimentFolder creatorFolder = new ExperimentFolder(folderId, "Creator Data", creatorUser, new ArrayList<>());

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(creatorFolder));

    // Scientist tries to add an experiment to Creator's folder
    assertThrows(AccessDeniedException.class, () ->
      folderService.addExperimentToFolder(folderId, sharedExperiment.getId(), scientistUser.getId())
    );

    Mockito.verify(folderRepository, Mockito.never()).save(any());
  }

  @Test
  void removeExperimentFromFolder_removesAndSaves() {
    UUID folderId = UUID.randomUUID();
    List<Experiment> exps = new ArrayList<>();
    exps.add(sharedExperiment);
    ExperimentFolder folder = new ExperimentFolder(folderId, "My Data", creatorUser, exps);

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

    GetFolderDto result = folderService.removeExperimentFromFolder(folderId, sharedExperiment.getId(), creatorUser.getId());

    assertTrue(result.experimentIds().isEmpty(), "Experiment should be removed");
    Mockito.verify(folderRepository).save(folder);
  }

  @Test
  void deleteFolder_whenAuthorized_deletesFolder() {
    UUID folderId = UUID.randomUUID();
    ExperimentFolder folder = new ExperimentFolder(folderId, "To Delete", creatorUser, new ArrayList<>());

    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

    folderService.deleteFolder(folderId, creatorUser.getId());

    ArgumentCaptor<ExperimentFolder> folderCaptor = ArgumentCaptor.forClass(ExperimentFolder.class);
    Mockito.verify(folderRepository).delete(folderCaptor.capture());
    assertEquals(folderId, folderCaptor.getValue().getId());
  }
}
