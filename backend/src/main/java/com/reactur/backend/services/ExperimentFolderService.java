package com.reactur.backend.services;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.dtos.folders.SaveFolderDto;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

/**
 * Service defining the business logic for managing personal experiment folders.
 * <p>
 * Enforces strict data isolation by ensuring users can only interact with folders
 * they explicitly own.
 * </p>
 */
public interface ExperimentFolderService {

  /**
   * Creates a new, empty folder owned by the requesting user.
   *
   * @param folderDto        the folder's name
   * @param requestingUserId the Keycloak ID of the user creating the folder
   * @return the created folder
   */
  GetFolderDto createFolder(SaveFolderDto folderDto, String requestingUserId);

  /**
   * Lists every folder owned by the requesting user.
   *
   * @param requestingUserId the Keycloak ID of the requesting user
   * @return the user's folders
   */
  List<GetFolderDto> getMyFolders(String requestingUserId);

  /**
   * Adds an experiment to one of the requesting user's own folders.
   * <p>
   * The experiment itself must also be one the requesting user created or is the designated
   * scientist for; there is no privileged bypass here, folders are strictly personal even for
   * ADMIN/SUPERVISOR. An experiment can only be in one of a given user's folders at a time, so
   * adding it here removes it from any of that user's other folders first.
   * </p>
   *
   * @param folderId         the UUID of the folder to add to; must be owned by the requesting user
   * @param experimentId     the UUID of the experiment to add; must be owned by the requesting user
   * @param requestingUserId the Keycloak ID of the requesting user
   * @return the updated folder
   * @throws ResourceNotFoundException if the folder or the experiment does not exist
   * @throws AccessDeniedException     if the requesting user does not own the folder or the experiment
   */
  GetFolderDto addExperimentToFolder(UUID folderId, UUID experimentId, String requestingUserId);

  /**
   * Removes an experiment from one of the requesting user's own folders. The experiment itself is
   * untouched, only the folder membership is removed.
   *
   * @param folderId         the UUID of the folder to remove from; must be owned by the requesting user
   * @param experimentId     the UUID of the experiment to remove
   * @param requestingUserId the Keycloak ID of the requesting user
   * @return the updated folder
   * @throws ResourceNotFoundException if the folder does not exist
   * @throws AccessDeniedException     if the requesting user does not own the folder
   */
  GetFolderDto removeExperimentFromFolder(UUID folderId, UUID experimentId, String requestingUserId);

  /**
   * Deletes one of the requesting user's own folders. Only the folder itself is deleted; the
   * experiments that were in it are untouched.
   *
   * @param folderId         the UUID of the folder to delete; must be owned by the requesting user
   * @param requestingUserId the Keycloak ID of the requesting user
   * @throws ResourceNotFoundException if the folder does not exist
   * @throws AccessDeniedException     if the requesting user does not own the folder
   */
  void deleteFolder(UUID folderId, String requestingUserId);
}
