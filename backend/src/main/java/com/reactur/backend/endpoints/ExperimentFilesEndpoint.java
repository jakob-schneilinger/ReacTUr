package com.reactur.backend.endpoints;


import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.services.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class ExperimentFilesEndpoint {

    // TODO: Pass and verify Security Context to SErvice

    /*
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> archiveFile(
            @PathVariable UUID fileId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        Role userRole = extractUserRole(authentication);
        fileService.archiveFile(fileId, jwt.getSubject(), userRole);

        return ResponseEntity.noContent().build();
    }
     */

    private final FileService fileService;

    @PostMapping(path = "/{experimentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperimentAttachmentDetailsDto> uploadFile(
            @PathVariable UUID experimentId,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.storeFile(experimentId, file));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID fileId) {
        ExperimentAttachmentDetailsDto details = fileService.getAttachmentDetail(fileId);
        Resource fileResource = fileService.loadFileAsResource(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(details.fileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + details.fileName() + "\"")
                .body(fileResource);
    }

    @GetMapping("/{fileId}/details")
    public ResponseEntity<ExperimentAttachmentDetailsDto> getFileDetails(
            @PathVariable UUID fileId) {

        return ResponseEntity.ok(fileService.getAttachmentDetail(fileId));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> archiveFile(
            @PathVariable UUID fileId) {
        fileService.archiveFile(fileId);
        return ResponseEntity.noContent().build();
    }
}
