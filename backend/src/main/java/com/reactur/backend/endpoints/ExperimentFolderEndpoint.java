package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.dtos.folders.SaveFolderDto;
import com.reactur.backend.services.ExperimentFolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing a user's personal experiment folders. Folders are private: a user
 * can only see and modify folders they own.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/folders")
public class ExperimentFolderEndpoint {

  private final ExperimentFolderService folderService;

  /**
   * Creates a new, empty folder owned by the requesting user.
   *
   * @param folderDto the folder's name
   * @param jwt       the Keycloak token containing the requesting user's internal ID
   * @return a {@link ResponseEntity} containing the created {@link GetFolderDto} and a 201
   *         Created status
   */
  @PostMapping
  public ResponseEntity<GetFolderDto> createFolder(
    @Valid @RequestBody SaveFolderDto folderDto,
    @AuthenticationPrincipal Jwt jwt
  ) {
    log.info("POST /api/folders called by User ID: {}", jwt.getSubject());
    GetFolderDto createdFolder = folderService.createFolder(folderDto, jwt.getSubject());
    return ResponseEntity.status(HttpStatus.CREATED).body(createdFolder);
  }

  /**
   * Lists every folder owned by the requesting user.
   *
   * @param jwt the Keycloak token containing the requesting user's internal ID
   * @return a {@link ResponseEntity} containing the user's {@link GetFolderDto}s
   */
  @GetMapping
  public ResponseEntity<List<GetFolderDto>> getMyFolders(
    @AuthenticationPrincipal Jwt jwt
  ) {
    log.info("GET /api/folders called by User ID: {}", jwt.getSubject());
    List<GetFolderDto> folders = folderService.getMyFolders(jwt.getSubject());
    return ResponseEntity.ok(folders);
  }

  /**
   * Adds an experiment to one of the requesting user's own folders. The experiment itself must
   * also be one the requesting user created or is the designated scientist for; there is no
   * privileged bypass here, folders are strictly personal even for ADMIN/SUPERVISOR. An
   * experiment can only be in one of a given user's folders at a time, so adding it here removes
   * it from any of that user's other folders first.
   *
   * @param folderId     the UUID of the folder to add to; must be owned by the requesting user
   * @param experimentId the UUID of the experiment to add; must be owned by the requesting user
   * @param jwt          the Keycloak token containing the requesting user's internal ID
   * @return a {@link ResponseEntity} containing the updated {@link GetFolderDto}
   */
  @PostMapping("/{folderId}/experiments/{experimentId}")
  public ResponseEntity<GetFolderDto> addExperimentToFolder(
    @PathVariable UUID folderId,
    @PathVariable UUID experimentId,
    @AuthenticationPrincipal Jwt jwt
  ) {
    log.info("POST /api/folders/{}/experiments/{} called by User ID: {}", folderId, experimentId, jwt.getSubject());
    GetFolderDto updatedFolder = folderService.addExperimentToFolder(folderId, experimentId, jwt.getSubject());
    return ResponseEntity.ok(updatedFolder);
  }

  /**
   * Removes an experiment from one of the requesting user's own folders. The experiment itself
   * is untouched, only the folder membership is removed.
   *
   * @param folderId     the UUID of the folder to remove from; must be owned by the requesting user
   * @param experimentId the UUID of the experiment to remove
   * @param jwt          the Keycloak token containing the requesting user's internal ID
   * @return a {@link ResponseEntity} containing the updated {@link GetFolderDto}
   */
  @DeleteMapping("/{folderId}/experiments/{experimentId}")
  public ResponseEntity<GetFolderDto> removeExperimentFromFolder(
    @PathVariable UUID folderId,
    @PathVariable UUID experimentId,
    @AuthenticationPrincipal Jwt jwt
  ) {
    log.info("DELETE /api/folders/{}/experiments/{} called by User ID: {}", folderId, experimentId, jwt.getSubject());
    GetFolderDto updatedFolder = folderService.removeExperimentFromFolder(folderId, experimentId, jwt.getSubject());
    return ResponseEntity.ok(updatedFolder);
  }

  /**
   * Deletes one of the requesting user's own folders. Only the folder itself is deleted; the
   * experiments that were in it are untouched.
   *
   * @param folderId the UUID of the folder to delete; must be owned by the requesting user
   * @param jwt      the Keycloak token containing the requesting user's internal ID
   * @return a {@link ResponseEntity} with a 204 No Content status
   */
  @DeleteMapping("/{folderId}")
  public ResponseEntity<Void> deleteFolder(
    @PathVariable UUID folderId,
    @AuthenticationPrincipal Jwt jwt
  ) {
    log.info("DELETE /api/folders/{} called by User ID: {}", folderId, jwt.getSubject());
    folderService.deleteFolder(folderId, jwt.getSubject());
    return ResponseEntity.noContent().build();
  }
}
