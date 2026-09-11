package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.dtos.folders.SaveFolderDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentFolder;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import com.reactur.backend.mappers.FolderMapper;
import com.reactur.backend.repositories.ExperimentFolderRepository;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.ExperimentFolderService;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
  private final ExperimentAuthorization experimentAuthorization;

  @Override
  @Transactional
  public GetFolderDto createFolder(SaveFolderDto folderDto, String requestingUserId) {
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
    return folderRepository.findAllByOwner_Id(requestingUserId)
      .stream()
      .map(folderMapper::folderToGetFolderDto)
      .toList();
  }

  @Override
  @Transactional
  public GetFolderDto addExperimentToFolder(UUID folderId, UUID experimentId, String requestingUserId) {
    ExperimentFolder folder = getFolderAndValidateOwnership(folderId, requestingUserId);

    // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): this used to be a
    // plain findById. Now it takes a pessimistic write lock on the experiment row, held until this method's
    // transaction commits, so a second concurrent addExperimentToFolder call for the SAME experiment (adding it to
    // a different folder of the same user) blocks here until this one finishes, then re-reads fresh folder
    // membership below instead of racing on stale data. Safe to hold: the rest of this method is pure local DB
    // reads/writes, no external I/O.
    Experiment experiment = experimentRepository.findByIdForUpdate(experimentId)
      .orElseThrow(() -> new ResourceNotFoundException("Experiment not found."));

    // No privileged bypass here by design
    // Folders are strictly personal. ADMIN/SUPERVISOR can only
    // add experiments into their OWN FOLDER (they created or are the scientist)
    if (!experimentAuthorization.isCreatorOrScientist(experiment, requestingUserId)) {
      log.warn("User {} tried to add non-owned experiment {} to folder", requestingUserId, experimentId);
      throw new AccessDeniedException("User not authorized to add a not-owned experiment to a folder");
    }

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


    // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): ExperimentFolder now
    // carries a @Version field, so a stale concurrent save() on this exact folder row fails loudly instead of
    // silently duplicating or losing data.
    // Prevent duplicates in the same folder
    boolean alreadyExists = folder.getExperiments().stream()
      .anyMatch(exp -> exp.getId().equals(experimentId));

    if (!alreadyExists) {
      folder.getExperiments().add(experiment);
      folderRepository.save(folder);
    }

    return folderMapper.folderToGetFolderDto(folder);
  }

  @Override
  @Transactional
  public GetFolderDto removeExperimentFromFolder(UUID folderId, UUID experimentId, String requestingUserId) {
    ExperimentFolder folder = getFolderAndValidateOwnership(folderId, requestingUserId);

    // I omit to check if the experiment is owned by the Person: If it's in its personal folder, is it to be
    // assumed that:
    // A) The ownership check was already performed to add it to the folder
    // B) In any case, folders are a personal thing. If for some modifications of any kind the person lost
    //    access to the experiment, it doesn't change anything that the personal folder of that user still
    //    contains the experiment

    folder.getExperiments().removeIf(exp -> exp.getId().equals(experimentId));

    // ExperimentFolder now carries a @Version field (see the Race condition inventory item in Software_todo.md),
    // so a genuine concurrent modification of this exact folder (e.g. addExperimentToFolder, or this same method
    // called again with a different experimentId) now makes this save() fail loudly instead of silently losing
    // data - which is the desired behavior for those cases. The one case that should NOT fail loudly is a harmless
    // double-submit of this exact same removal (e.g. a network retry): both calls compute the identical resulting
    // state (the experiment removed), so treat a lock conflict here as "someone already did this", not an error.
    try {
      folderRepository.save(folder);
    } catch (ObjectOptimisticLockingFailureException e) {
      log.debug("Folder {} was modified concurrently while removing experiment {}; re-reading current state.",
              folderId, experimentId);
      folder = folderRepository.findById(folderId)
              .orElseThrow(() -> new ResourceNotFoundException("Folder not found."));
    }

    return folderMapper.folderToGetFolderDto(folder);
  }

  @Override
  @Transactional
  public void deleteFolder(UUID folderId, String requestingUserId) {
    // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): ExperimentFolder now
    // carries a @Version field. Hibernate includes the version in the DELETE statement's WHERE clause too, so if
    // addExperimentToFolder or removeExperimentFromFolder already modified this exact folder (bumping its version)
    // after it was fetched here, this delete affects zero rows and Hibernate throws
    // ObjectOptimisticLockingFailureException instead of silently deleting stale data or being silently undone by
    // the other call's later save(). Deliberately NOT caught here (unlike the benign double-remove case above) -
    // "someone just modified the folder you're trying to delete" is a genuine conflict worth surfacing as a 409.
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
