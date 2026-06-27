package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.ExperimentDtos.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.ReactantDtos.StructureDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/structure/file")
@RequiredArgsConstructor
public class StructureFilesEndpoint {

    @PostMapping(path = "/{reactantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExperimentAttachmentDetailsDto> uploadStructure(
            @PathVariable UUID reactantId,
            @RequestParam("file") MultipartFile file) {

        // TODO: Implement
        return null;
    }

    @PostMapping("/{reactantId}/generate")
    public ResponseEntity<Void> generateStructureFromAPI(@PathVariable UUID reactantId) {

        //TODO: Do generate Structure From API

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{structureId}/select")
    public ResponseEntity<StructureDto> selectStructure(@PathVariable UUID structureId) {
        //TODO: Do generate Structure From API

        return ResponseEntity.noContent().build();
    }
}
