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

    // TODO-CLAUDE (next thread - Reactant/Structure permissions): change to
    // @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')"). Per team decision, SUPERVISOR has the
    // same structure-management rights as ADMIN (upload/generate/select), mirroring their ability
    // to create reactants - otherwise a SUPERVISOR-created reactant could never get an ACTIVE
    // structure and would be stuck waiting on an Admin.
    @PostMapping(path = "/{reactantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StructureDto> uploadStructure(
            @PathVariable UUID reactantId,
            @RequestParam("file") MultipartFile file) {

        StructureDto structureDto = structureService.uploadStructure(reactantId, file);
        return ResponseEntity.ok(structureDto);
    }

    // TODO-CLAUDE (next thread): change to @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')") - see note above uploadStructure.
    @PostMapping("/{reactantId}/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> generateStructureFromAPI(@PathVariable UUID reactantId) {

        structureService.generateStructureFromAPI(reactantId);
        return ResponseEntity.noContent().build();
    }

    // TODO-CLAUDE (next thread): change to @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')") - see
    // note above uploadStructure. Confirmed explicitly: select should also be open to SUPERVISOR.
    @PostMapping("/{structureId}/select")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StructureDto> selectStructure(@PathVariable UUID structureId) {

        StructureDto structureDto = structureService.selectStructure(structureId);
        return ResponseEntity.ok(structureDto);
    }
}
