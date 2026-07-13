package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.dtos.folders.SaveFolderDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentFolder;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.FolderMapper;
import com.reactur.backend.repositories.ExperimentFolderRepository;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.ExperimentFolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ExperimentFolderServiceImpl implements ExperimentFolderService {

  private final ExperimentFolderRepository folderRepository;
  private final ExperimentRepository experimentRepository;
  private final UserRepository userRepository;
  private final FolderMapper folderMapper;

  @Override
  @Transactional
  public GetFolderDto createFolder(SaveFolderDto folderDto, String requestingUserId) {
    log.trace("Creating new folder for user: {}", requestingUserId);

    User owner = userRepository.findById(requestingUserId)
      .orElseThrow(() -> new IllegalStateException("Authenticated user lacks internal profile."));

    ExperimentFolder newFolder = ExperimentFolder.builder()
      .name(folderDto.name())
      .owner(owner)
      .build();

    ExperimentFolder savedFolder = folderRepository.save(newFolder);
    return folderMapper.folderToGetFolderDto(savedFolder);
  }

  @Override
  public List<GetFolderDto> getMyFolders(String requestingUserId) {
    log.trace("Fetching folders for user: {}", requestingUserId);

    return folderRepository.findAllByOwner_Id(requestingUserId)
      .stream()
      .map(folderMapper::folderToGetFolderDto)
      .toList();
  }

  @Override
  @Transactional
  public GetFolderDto addExperimentToFolder(UUID folderId, UUID experimentId, String requestingUserId) {
    log.trace("Adding experiment {} to folder {}", experimentId, folderId);

    ExperimentFolder folder = getFolderAndValidateOwnership(folderId, requestingUserId);

    // One Folder Per User: Find all user's folders and remove this experiment
    List<ExperimentFolder> allUserFolders = folderRepository.findAllByOwner_Id(requestingUserId);
    for (ExperimentFolder oldFolder : allUserFolders) {
      if (!oldFolder.getId().equals(folderId)) {
        boolean removed = oldFolder.getExperiments().removeIf(exp -> exp.getId().equals(experimentId));
        if (removed) {
          folderRepository.save(oldFolder); // Update DB to drop old relationship
          log.debug("Removed experiment {} from previous folder {}", experimentId, oldFolder.getId());
        }
      }
    }


    // Prevent duplicates in the same folder
    boolean alreadyExists = folder.getExperiments().stream()
      .anyMatch(exp -> exp.getId().equals(experimentId));

    if (!alreadyExists) {
      Experiment experiment = experimentRepository.findById(experimentId)
        .orElseThrow(() -> new ResourceNotFoundException("Experiment not found."));

      folder.getExperiments().add(experiment);
      folderRepository.save(folder);
    }

    return folderMapper.folderToGetFolderDto(folder);
  }

  @Override
  @Transactional
  public GetFolderDto removeExperimentFromFolder(UUID folderId, UUID experimentId, String requestingUserId) {
    log.trace("Removing experiment {} from folder {}", experimentId, folderId);

    ExperimentFolder folder = getFolderAndValidateOwnership(folderId, requestingUserId);

    folder.getExperiments().removeIf(exp -> exp.getId().equals(experimentId));
    folderRepository.save(folder);

    return folderMapper.folderToGetFolderDto(folder);
  }

  @Override
  @Transactional
  public void deleteFolder(UUID folderId, String requestingUserId) {
    log.trace("Deleting folder {}", folderId);

    ExperimentFolder folder = getFolderAndValidateOwnership(folderId, requestingUserId);

    folderRepository.delete(folder);
  }

  private ExperimentFolder getFolderAndValidateOwnership(UUID folderId, String requestingUserId) {
    ExperimentFolder folder = folderRepository.findById(folderId)
      .orElseThrow(() -> new ResourceNotFoundException("Folder not found."));

    if (!folder.getOwner().getId().equals(requestingUserId)) {
      log.warn("User {} attempted to modify folder {} owned by {}",
        requestingUserId, folderId, folder.getOwner().getId());
      throw new AccessDeniedException("You do not have permission to access this folder.");
    }

    return folder;
  }
}
