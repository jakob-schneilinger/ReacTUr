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
import com.reactur.backend.services.authorization.ExperimentAuthorization;
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
  private final ExperimentAuthorization experimentAuthorization;

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

    Experiment experiment = experimentRepository.findById(experimentId)
      .orElseThrow(() -> new ResourceNotFoundException("Experiment not found."));

    // No privileged bypass here by design
    // Folders are strictly personal. ADMIN/SUPERVISOR can only
    // add experiments into their OWN FOLDER (they created or are the scientist)
    if (!experimentAuthorization.isCreatorOrScientist(experiment, requestingUserId)) {
      log.warn("User {} tried to add non-owned experiment {} to folder", requestingUserId, experimentId);
      throw new AccessDeniedException("User not authorized to add a not-owned experiment to a folder");
    }

    // TODO-CLAUDE (race condition inventory, Family 1 - fixable without migration tooling, see
    // docs/Software_todo.md "Race condition inventory" under the ReactantRepository.hasNameConflict item): this
    // dedup pass is scoped to one user's folders (not a whole-table scan), and the invariant it protects is anchored
    // to one specific, already-existing row: the Experiment being added (by experimentId). Two concurrent
    // addExperimentToFolder calls for the SAME experiment but TWO DIFFERENT folders of the SAME user can each observe
    // "not currently in any other folder of mine" and both add it, leaving the experiment in two folders for that
    // user simultaneously - violating the "one folder per user" invariant this loop exists to enforce. Fix: lock the
    // Experiment row (e.g. @Lock(LockModeType.PESSIMISTIC_WRITE) on its lookup) so a second concurrent call re-reads
    // fresh folder membership before acting - there's no dedicated join-table entity to lock directly since
    // `experiments` is a plain @ManyToMany @JoinTable.
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


    // TODO-CLAUDE (race condition inventory, Family 1 - fixable without migration tooling, see
    // docs/Software_todo.md "Race condition inventory" under the ReactantRepository.hasNameConflict item): targets
    // one specific, already-fetched ExperimentFolder row (this folder, by id). Two concurrent adds of the same
    // experiment to the same folder could both pass this check and both add it - whether that actually corrupts data
    // depends on whether the folder_experiments join table has a composite unique constraint on
    // (folder_id, experiment_id); if it does, the losing save() just fails loudly at flush time instead of silently
    // duplicating, which is an acceptable if slightly ungraceful outcome. Fix if needed: @Lock(PESSIMISTIC_WRITE) on
    // this folder's lookup, or @Version on ExperimentFolder.
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
    log.trace("Removing experiment {} from folder {}", experimentId, folderId);

    ExperimentFolder folder = getFolderAndValidateOwnership(folderId, requestingUserId);

    // I omit to check if the experiment is owned by the Person: If it's in its personal folder, is it to be
    // assumed that:
    // A) The ownership check was already performed to add it to the folder
    // B) In any case, folders are a personal thing. If for some modifications of any kind the person lost
    //    access to the experiment, it doesn't change anything that the personal folder of that user still
    //    contains the experiment

    folder.getExperiments().removeIf(exp -> exp.getId().equals(experimentId));
    folderRepository.save(folder);

    return folderMapper.folderToGetFolderDto(folder);
  }

  @Override
  @Transactional
  public void deleteFolder(UUID folderId, String requestingUserId) {
    log.trace("Deleting folder {}", folderId);

    // TODO-CLAUDE (race condition inventory, Family 1 - fixable without migration tooling, see
    // docs/Software_todo.md "Race condition inventory" under the ReactantRepository.hasNameConflict item): targets
    // one specific, already-existing ExperimentFolder row (by id). If addExperimentToFolder or
    // removeExperimentFromFolder fetches this exact folder, then this method deletes it, then that other method's
    // own save() commits afterward, JPA's save()/merge() on an entity with a pre-assigned id that no longer exists in
    // the DB re-inserts it - i.e. this delete can be silently undone by a resurrection from a concurrent
    // add/remove-experiment call that read the folder just before the delete. Fix: @Lock(LockModeType.PESSIMISTIC_WRITE)
    // on the folder lookup in this method (and ideally in addExperimentToFolder/removeExperimentFromFolder too, so
    // they serialize against each other).
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
