package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.services.StructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/structure")
@RequiredArgsConstructor
public class StructureEndpoint {

    private final StructureService structureService;

    @PostMapping(path = "/{reactantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<StructureDto> uploadStructure(
            @PathVariable UUID reactantId,
            @RequestParam("file") MultipartFile file) {

        StructureDto structureDto = structureService.uploadStructure(reactantId, file);
        return ResponseEntity.ok(structureDto);
    }

    @PostMapping("/{reactantId}/generate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<Void> generateStructureFromAPI(@PathVariable UUID reactantId) {

        structureService.generateStructureFromAPI(reactantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{structureId}/select")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<StructureDto> selectStructure(@PathVariable UUID structureId) {

        StructureDto structureDto = structureService.selectStructure(structureId);
        return ResponseEntity.ok(structureDto);
    }
}
