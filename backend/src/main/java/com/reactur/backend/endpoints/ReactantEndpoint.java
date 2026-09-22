package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.UpdateReactantDto;
import com.reactur.backend.services.ReactantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing reactants.
 */
@Slf4j
@RestController
@RequestMapping("/api/reactant")
@RequiredArgsConstructor
public class ReactantEndpoint {

    private final ReactantService reactantService;

    /**
     * Creates a new reactant. Only ADMIN and SUPERVISOR may create reactants.
     *
     * @param saveReactant the reactant to create
     * @param jwt          the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the created {@link ReactantDetailDto} and a 201
     *         Created status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ReactantDetailDto> createReactant(@Valid @RequestBody CreateReactantDto saveReactant, @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /api/reactant called by user {}. ShortName: {}", jwt.getSubject(), saveReactant.shortName());
        ReactantDetailDto created = reactantService.createReactant(saveReactant, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Partially updates an existing reactant. Only ADMIN may update a reactant.
     *
     * @param uuid         the UUID of the reactant to update
     * @param saveReactant the fields to change; unset fields are left as they are
     * @param jwt          the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the updated {@link ReactantDetailDto}
     */
    @PatchMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReactantDetailDto> updateReactant(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateReactantDto saveReactant,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PATCH /api/reactant/{} called by user {}", uuid, jwt.getSubject());
        ReactantDetailDto updated = reactantService.updateReactant(uuid, saveReactant);
        return ResponseEntity.ok(updated);
    }

    /**
     * Retrieves the full details of a reactant, including its structures. Visible to any
     * approved user.
     *
     * @param uuid the UUID of the reactant
     * @param jwt  the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the {@link ReactantDetailDto}
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<ReactantDetailDto> getReactantDetail(@PathVariable UUID uuid, @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/reactant/{} called by user {}", uuid, jwt.getSubject());
        ReactantDetailDto detail = reactantService.getReactantDetail(uuid);
        return ResponseEntity.ok(detail);
    }

    /**
     * Lists all active (non-archived) reactants. Visible to any approved user.
     *
     * @param jwt the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the active {@link ReactantDto}s
     */
    @GetMapping
    public ResponseEntity<List<ReactantDto>> getAllReactants(@AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/reactant called by user {}", jwt.getSubject());
        List<ReactantDto> reactants = reactantService.getAllReactants();
        return ResponseEntity.ok(reactants);
    }

    /**
     * Lists all archived reactants. Only ADMIN and SUPERVISOR may view archived reactants.
     *
     * @param jwt the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the archived {@link ReactantDto}s
     */
    @GetMapping("/archived")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<ReactantDto>> getArchivedReactants(@AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/reactant/archived called by user {}", jwt.getSubject());
        List<ReactantDto> archived = reactantService.getArchivedReactants();
        return ResponseEntity.ok(archived);
    }

    /**
     * Archives (soft-deletes) a reactant. Only ADMIN may archive a reactant.
     *
     * @param uuid the UUID of the reactant to archive
     * @param jwt  the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} with a 204 No Content status
     */
    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archiveReactant(@PathVariable UUID uuid, @AuthenticationPrincipal Jwt jwt) {
        log.info("DELETE /api/reactant/{} called by user {}", uuid, jwt.getSubject());
        reactantService.archiveReactant(uuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a previously archived reactant. Treated as archive-management, so it stays
     * ADMIN-only even though SUPERVISOR may create reactants.
     *
     * @param uuid the UUID of the archived reactant to restore
     * @param jwt  the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the restored {@link ReactantDto}
     */
    @PutMapping("/{uuid}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReactantDto> restoreReactant(@PathVariable UUID uuid, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /api/reactant/{}/restore called by user {}", uuid, jwt.getSubject());
        ReactantDto restored = reactantService.restoreReactant(uuid);
        return ResponseEntity.ok(restored);
    }
}
