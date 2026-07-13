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

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/folders")
public class ExperimentFolderEndpoint {

  private final ExperimentFolderService folderService;

  @PostMapping
  public ResponseEntity<GetFolderDto> createFolder(
    @Valid @RequestBody SaveFolderDto folderDto,
    @AuthenticationPrincipal Jwt jwt
  ) {
    log.info("POST /api/folders called by User ID: {}", jwt.getSubject());
    GetFolderDto createdFolder = folderService.createFolder(folderDto, jwt.getSubject());
    return ResponseEntity.status(HttpStatus.CREATED).body(createdFolder);
  }

  @GetMapping
  public ResponseEntity<List<GetFolderDto>> getMyFolders(
    @AuthenticationPrincipal Jwt jwt
  ) {
    log.info("GET /api/folders called by User ID: {}", jwt.getSubject());
    List<GetFolderDto> folders = folderService.getMyFolders(jwt.getSubject());
    return ResponseEntity.ok(folders);
  }

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
