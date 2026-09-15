package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.security.RoleExtractor;
import com.reactur.backend.services.ExperimentHistoryService;
import com.reactur.backend.services.ExperimentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing experimental data within the ReacTUr chemical reaction knowledge base.
 * <p>
 * This endpoint provides the external API boundaries for creating, retrieving, updating, and dynamically
 * querying chemical synthesis experiments. It ensures that active experiments remain globally searchable to prevent
 * duplicated lab work, while restricting the modification and viewing of archived records to authorized personnel
 * (The creator who included the experiment in the database, the designated scientist, or system administrators).
 * </p>
 *
 * @see com.reactur.backend.services.ExperimentService
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/experiment")
public class ExperimentEndpoint {
    private final ExperimentService experimentService;
    private final ExperimentHistoryService experimentHistoryService;
    private final RoleExtractor roleExtractor;


    /**
     * Records a new chemical experiment in the knowledge base.
     * <p>
     * The user initiating the request is permanently recorded as the creator. This field
     * is immutable. Semantic validation is performed to ensure all referenced chemical
     * entities (reactants, solvents, instruments) exist in the system prior to saving.
     * </p>
     *
     * @param experimentDto  The payload containing the experimental setup, metadata, and kinetics results.
     * @param authentication The security context containing the user's role authorities.
     * @param jwt            The Keycloak token containing the requesting user's internal ID.
     * @return A {@link ResponseEntity} containing the fully populated {@link GetExperimentDto} and a 201 Created status.
     */
    @PostMapping
    public ResponseEntity<GetExperimentDto> createExperiment(
      @Valid @RequestBody SaveExperimentDto experimentDto,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("POST /api/experiment called by User ID: {}. ReactantA: {}, ReactantB: {}, DOIs: {}",
          jwt.getSubject(), experimentDto.reactantA(), experimentDto.reactantB(), experimentDto.dois());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto createdExperiment = experimentService.createExperiment(experimentDto, jwt.getSubject(), userRole);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdExperiment);
    }

    /**
     * Performs a full replacement of the mutable data for an existing active experiment.
     * <p>
     * Only the original creator, the tagged scientist, or a system administrator
     * are authorized to update an experiment.
     * <br>
     * Archived experiments are frozen and will trigger a 409 Conflict if an update
     * is attempted. They must be restored first. The {@code createdBy} and {@code archivedAt} fields
     * remain immutable during this operation.
     * </p>
     *
     * @param uuid           The unique identifier of the experiment to update.
     * @param experimentDto  The payload containing the updated experimental setup and results.
     * @param authentication The security context containing the user's role authorities.
     * @param jwt            The Keycloak token containing the requesting user's internal ID.
     * @return A {@link ResponseEntity} containing the updated {@link GetExperimentDto}.
     */
    @PutMapping("/{uuid}")
    public ResponseEntity<GetExperimentDto> updateExperiment(
      @PathVariable UUID uuid,
      @Valid @RequestBody SaveExperimentDto experimentDto,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("PUT /api/experiment/{} called by User ID: {}", uuid, jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto updatedExperiment = experimentService.updateExperiment(uuid, experimentDto, jwt.getSubject(), userRole);

        return ResponseEntity.ok(updatedExperiment);
    }

    /**
     * Retrieves the details of a specific experiment by its UUID.
     * <p>
     * Active experiments are globally visible to all authenticated users.
     * However, if the requested experiment has been archived, access is strictly restricted to
     * the original creator, the designated scientist, or a system administrator.
     * </p>
     *
     * @param uuid           The unique identifier of the experiment.
     * @param authentication The security context containing the user's role authorities.
     * @param jwt            The Keycloak token containing the requesting user's internal ID.
     * @return A {@link ResponseEntity} containing the requested {@link GetExperimentDto}.
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<GetExperimentDto> getExperiment(
      @PathVariable UUID uuid,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("GET /api/experiment/{} called by User ID: {}", uuid, jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto experiment = experimentService.getExperiment(uuid, jwt.getSubject(), userRole);

        return ResponseEntity.ok(experiment);
    }

    /**
     * Searches and filters active experiments across the knowledge base.
     * <p>
     * This endpoint retrieves a paginated list of <b>active</b> experiments (archived records are
     * excluded from these results). It supports complex filtering based on temporal ranges,
     * chemical properties, and kinetics results.
     * <br>
     * <b>Note on Reactants:</b> Reactant searches evaluate commutatively. Querying for Reactant A
     * and Reactant B will return matches where the reactants were recorded in either order.
     * </p>
     *
     * @param searchParams   The DTO encapsulating the requested filter criteria.
     * @param pageable       The Spring Pageable object dictating page size, index, and sorting.
     * @param authentication The security context containing the user's role authorities.
     * @param jwt            The Keycloak token containing the requesting user's internal ID.
     * @return A {@link ResponseEntity} containing a {@link Page} of matching {@link GetExperimentDto}s.
     */
    @GetMapping()
    public ResponseEntity<Page<GetExperimentDto>> searchExperiments(
      @Valid SearchParametersDto searchParams,
      Pageable pageable,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("GET /api/experiment (search) called by User ID: {}", jwt.getSubject());

        Role userRole = extractUserRole(authentication);

        Page<GetExperimentDto> searchResults =
          experimentService.searchExperiments(searchParams, pageable, jwt.getSubject(), userRole);

        return ResponseEntity.ok(searchResults);
    }

    /**
     * Retrieves the chronological audit trail for a specific experiment.
     * <p>
     * Active experiments are globally visible to all authenticated users.
     * However, if the requested experiment has been archived, access is restricted to
     * the original creator, the designated scientist, or a system administrator.
     * </p>
     *
     * @param uuid           The unique identifier of the experiment.
     * @param authentication The security context containing the user's role authorities.
     * @param jwt            The Keycloak token containing the requesting user's internal ID.
     * @return A {@link ResponseEntity} containing the {@link ExperimentHistoryDto} history entries.
     */
    @GetMapping("/{uuid}/history")
    public ResponseEntity<List<ExperimentHistoryDto>> getAuditLogs(
      @PathVariable UUID uuid,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("GET /api/experiment/{}/history called by User ID: {}", uuid, jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        List<ExperimentHistoryDto> history = experimentHistoryService.getExperimentHistory(uuid, jwt.getSubject(), userRole);

        return ResponseEntity.ok(history);
    }

    /**
     * Retrieves a list of archived experiments based on the user's authorization level.
     * <p>
     * Standard users will only receive archived experiments where they
     * are either the original creator or the designated scientist. Users with the {@code ADMIN}
     * role will retrieve all archived experiments across the entire system.
     * </p>
     *
     * @param authentication The security context containing the user's role authorities.
     * @param jwt            The Keycloak token containing the requesting user's internal ID.
     * @return A {@link ResponseEntity} containing an array of {@link GetExperimentDto}s.
     */
    @GetMapping("/archived")
    public ResponseEntity<GetExperimentDto[]> getArchivedExperiments(
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("GET /api/experiment/archived called by User ID: {}", jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto[] archivedExperiments = experimentService.getArchivedExperiments(jwt.getSubject(), userRole);

        return ResponseEntity.ok(archivedExperiments);
    }

    /**
     * Soft-deletes an active experiment by stamping it with an archive timestamp.
     * <p>
     * This operation removes the experiment from public visibility and standard search results.
     * The data is preserved in the database to maintain referential integrity and audit history.
     * <br>
     * Only the original creator, the tagged scientist, or a system
     * administrator are authorized to archive an experiment.
     * </p>
     *
     * @param uuid           The unique identifier of the experiment to archive.
     * @param authentication The security context containing the user's role authorities.
     * @param jwt            The Keycloak token containing the requesting user's internal ID.
     * @return A {@link ResponseEntity} with a 204 No Content status upon successful archiving.
     */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> archiveExperiment(
      @PathVariable UUID uuid,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("DELETE (Archive) /api/experiment/{} called by User ID: {}", uuid, jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        experimentService.archiveExperiment(uuid, jwt.getSubject(), userRole);

        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a previously archived experiment back to an active state.
     * <p>
     * This operation clears the archive timestamp, making the experiment globally visible
     * and discoverable in standard searches once again.
     * <br>
     * Only the original creator, the tagged scientist, or a system
     * administrator are authorized to restore an experiment.
     * </p>
     *
     * @param uuid           The unique identifier of the archived experiment to restore.
     * @param authentication The security context containing the user's role authorities.
     * @param jwt            The Keycloak token containing the requesting user's internal ID.
     * @return A {@link ResponseEntity} containing the restored {@link GetExperimentDto}.
     */
    @PutMapping("/{uuid}/restore")
    public ResponseEntity<GetExperimentDto> restoreExperiment(
      @PathVariable UUID uuid,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("PUT (Restore) /api/experiment/{} called by User ID: {}", uuid, jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto restoredExperiment = experimentService.restoreExperiment(uuid, jwt.getSubject(), userRole);

        return ResponseEntity.ok(restoredExperiment);
    }

    // Delegates to the shared RoleExtractor so every endpoint that needs a requestingUserRole
    // derives it identically
    private Role extractUserRole(Authentication authentication) {
        return roleExtractor.extractRole(authentication);
    }
}
