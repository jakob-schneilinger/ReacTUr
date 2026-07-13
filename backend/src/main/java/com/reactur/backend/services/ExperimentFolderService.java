package com.reactur.backend.services;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.dtos.folders.SaveFolderDto;

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

  GetFolderDto createFolder(SaveFolderDto folderDto, String requestingUserId);

  List<GetFolderDto> getMyFolders(String requestingUserId);

  GetFolderDto addExperimentToFolder(UUID folderId, UUID experimentId, String requestingUserId);

  GetFolderDto removeExperimentFromFolder(UUID folderId, UUID experimentId, String requestingUserId);

  void deleteFolder(UUID folderId, String requestingUserId);
}
