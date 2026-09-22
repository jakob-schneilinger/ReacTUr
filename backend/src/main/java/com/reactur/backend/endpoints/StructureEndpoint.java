package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.services.StructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for uploading and managing 3D structures for a reactant. Only ADMIN and
 * SUPERVISOR may manage structures.
 */
@Slf4j
@RestController
@RequestMapping("/api/structure")
@RequiredArgsConstructor
public class StructureEndpoint {

    private final StructureService structureService;

    /**
     * Uploads a structure file manually for a reactant. The uploaded structure becomes that
     * reactant's ACTIVE structure immediately.
     *
     * @param reactantId the UUID of the reactant to upload a structure for
     * @param file       the structure file
     * @param jwt        the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the saved {@link StructureDto}
     */
    @PostMapping(path = "/{reactantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<StructureDto> uploadStructure(
            @PathVariable UUID reactantId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("POST /api/structure/{} called by user {}. FileName: {}", reactantId, jwt.getSubject(), file.getOriginalFilename());

        StructureDto structureDto = structureService.uploadStructure(reactantId, file);
        return ResponseEntity.ok(structureDto);
    }

    /**
     * Triggers structure generation for a reactant via the external structure-generation API.
     * Creates a PENDING structure record with the external job ID; the result is picked up later
     * by the scheduled polling job.
     *
     * @param reactantId the UUID of the reactant whose SMILES should be used
     * @param jwt        the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} with a 204 No Content status
     */
    @PostMapping("/{reactantId}/generate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<Void> generateStructureFromAPI(@PathVariable UUID reactantId, @AuthenticationPrincipal Jwt jwt) {

        log.info("POST /api/structure/{}/generate called by user {}", reactantId, jwt.getSubject());

        structureService.generateStructureFromAPI(reactantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Selects a structure to become the reactant's ACTIVE structure, rejecting whichever other
     * structure of the same reactant was previously ACTIVE.
     *
     * @param structureId the UUID of the structure to activate
     * @param jwt         the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the updated {@link StructureDto}
     */
    @PostMapping("/{structureId}/select")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<StructureDto> selectStructure(@PathVariable UUID structureId, @AuthenticationPrincipal Jwt jwt) {

        log.info("POST /api/structure/{}/select called by user {}", structureId, jwt.getSubject());

        StructureDto structureDto = structureService.selectStructure(structureId);
        return ResponseEntity.ok(structureDto);
    }
}
