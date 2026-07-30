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

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class ExperimentFilesEndpoint {

    private final FileService fileService;
    private final RoleExtractor roleExtractor;

    @PostMapping(path = "/{experimentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperimentAttachmentDetailsDto> uploadFile(
            @PathVariable UUID experimentId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        Role userRole = roleExtractor.extractRole(authentication);
        ExperimentAttachmentDetailsDto stored = fileService.storeFile(experimentId, file, jwt.getSubject(), userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(stored);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID fileId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

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

    @GetMapping("/{fileId}/details")
    public ResponseEntity<ExperimentAttachmentDetailsDto> getFileDetails(
            @PathVariable UUID fileId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        Role userRole = roleExtractor.extractRole(authentication);
        return ResponseEntity.ok(fileService.getAttachmentDetail(fileId, jwt.getSubject(), userRole));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> archiveFile(
            @PathVariable UUID fileId,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        Role userRole = roleExtractor.extractRole(authentication);
        fileService.archiveFile(fileId, jwt.getSubject(), userRole);
        return ResponseEntity.noContent().build();
    }
}
