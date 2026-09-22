package com.reactur.backend.endpoints;


import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.security.RoleExtractor;
import com.reactur.backend.services.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for uploading, downloading, and archiving files attached to an experiment.
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class ExperimentFilesEndpoint {

    private final FileService fileService;
    private final RoleExtractor roleExtractor;

    /**
     * Uploads a file and attaches it to an experiment.
     * <p>
     * Only the experiment's creator, its designated scientist, or a privileged role may upload to
     * it, and never to an archived experiment.
     *
     * @param experimentId   the UUID of the experiment to attach the file to
     * @param file           the uploaded file
     * @param authentication the security context containing the user's role authorities
     * @param jwt            the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the saved {@link ExperimentAttachmentDetailsDto}
     *         and a 201 Created status
     */
    @PostMapping(path = "/{experimentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperimentAttachmentDetailsDto> uploadFile(
            @PathVariable UUID experimentId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("POST /api/file/{} called by user {}. FileName: {}", experimentId, jwt.getSubject(), file.getOriginalFilename());

        Role userRole = roleExtractor.extractRole(authentication);
        ExperimentAttachmentDetailsDto stored = fileService.storeFile(experimentId, file, jwt.getSubject(), userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(stored);
    }

    /**
     * Downloads a file's raw bytes as an attachment.
     * <p>
     * Visible to any approved user while the parent experiment is active, and restricted to its
     * creator, designated scientist, or a privileged role once archived.
     *
     * @param fileId         the UUID of the file to download
     * @param authentication the security context containing the user's role authorities
     * @param jwt            the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} streaming the file bytes with a Content-Disposition
     *         attachment header
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID fileId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("GET /api/file/{} called by user {}", fileId, jwt.getSubject());

        Role userRole = roleExtractor.extractRole(authentication);
        ExperimentAttachmentDetailsDto details = fileService.getAttachmentDetail(fileId, jwt.getSubject(), userRole);
        Resource fileResource = fileService.loadFileAsResource(fileId, jwt.getSubject(), userRole);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(details.fileName())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(details.fileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(fileResource);
    }

    /**
     * Retrieves a file's metadata (name, type, size, checksum, download URL) without its bytes.
     * Follows the same view rules as {@link #downloadFile}.
     *
     * @param fileId         the UUID of the file
     * @param authentication the security context containing the user's role authorities
     * @param jwt            the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the {@link ExperimentAttachmentDetailsDto}
     */
    @GetMapping("/{fileId}/details")
    public ResponseEntity<ExperimentAttachmentDetailsDto> getFileDetails(
            @PathVariable UUID fileId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("GET /api/file/{}/details called by user {}", fileId, jwt.getSubject());

        Role userRole = roleExtractor.extractRole(authentication);
        return ResponseEntity.ok(fileService.getAttachmentDetail(fileId, jwt.getSubject(), userRole));
    }

    /**
     * Archives (soft-deletes) a file.
     * <p>
     * Only the parent experiment's creator, its designated scientist, or a privileged role may
     * archive a file, and never on an already-archived experiment.
     *
     * @param fileId         the UUID of the file to archive
     * @param authentication the security context containing the user's role authorities
     * @param jwt            the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} with a 204 No Content status
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> archiveFile(
            @PathVariable UUID fileId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("DELETE /api/file/{} called by user {}", fileId, jwt.getSubject());

        Role userRole = roleExtractor.extractRole(authentication);
        fileService.archiveFile(fileId, jwt.getSubject(), userRole);
        return ResponseEntity.noContent().build();
    }
}
