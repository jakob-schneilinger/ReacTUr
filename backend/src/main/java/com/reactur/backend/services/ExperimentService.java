package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.UnprocessableEntityException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * This service takes care of the experiment's information data transfer, semantic validation of
 * chemical entities, and role-based access control to ensure efficient and secure
 * experiment management.
 * </p>
 */
@Service
public interface ExperimentService {

  /**
   * Validates and persists a new chemical experiment.
   * <p>
   * Enforces semantic validation across all associated lookup entities (reactants, solvents, etc.).
   * The {@code requestingUserId} is permanently bound as the immutable creator of the record.
   * </p>
   *
   * @param experimentDto      The payload containing the experimental setup and results.
   * @param requestingUserId   The internal ID of the authenticated user performing the action.
   * @param requestingUserRole The role of the authenticated user.
   * @return A mapped {@link GetExperimentDto} of the persisted entity.
   * @throws UnprocessableEntityException if any referenced chemical entities do not exist.
   */
  GetExperimentDto createExperiment(SaveExperimentDto experimentDto, String requestingUserId, Role requestingUserRole);

  /**
   * Retrieves an experiment by its unique identifier.
   * <p>
   * Active experiments are accessible to all roles. If the experiment
   * is archived, the requesting user must be the creator, the designated scientist, or an admin.
   * </p>
   *
   * @param experimentId       The UUID of the target experiment.
   * @param requestingUserId   The internal ID of the authenticated user.
   * @param requestingUserRole The role of the authenticated user.
   * @return The requested {@link GetExperimentDto}.
   * @throws AccessDeniedException if attempting to access an unauthorized archived record.
   */
  GetExperimentDto getExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole);

  /**
   * Soft-deletes an experiment by setting its archive timestamp.
   * The requesting user must be the creator, the designated scientist, or an admin.
   *
   * @param experimentId       The UUID of the experiment to archive.
   * @param requestingUserId   The internal ID of the authenticated user.
   * @param requestingUserRole The role of the authenticated user.
   * @throws AccessDeniedException if the user lacks ownership or admin rights.
   */
  void archiveExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole);

  /**
   * Restores an archived experiment to active status by clearing its archive timestamp.
   * The requesting user must be the creator, the designated scientist, or an admin.
   *
   * @param experimentId       The UUID of the experiment to restore.
   * @param requestingUserId   The internal ID of the authenticated user.
   * @param requestingUserRole The role of the authenticated user.
   * @return The restored {@link GetExperimentDto}.
   * @throws AccessDeniedException if the user lacks ownership or admin rights.
   */
  GetExperimentDto restoreExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole);

  /**
   * Retrieves all archived experiments permitted for the requesting user.
   * <p>
   * Admin roles retrieve the global archive list. Standard roles retrieve a list
   * containing only the archived experiments they created or were designated as the scientist for.
   * </p>
   *
   * @param requestingUserId   The internal ID of the authenticated user.
   * @param requestingUserRole The role of the authenticated user.
   * @return An array of {@link GetExperimentDto}s.
   */
  GetExperimentDto[] getArchivedExperiments(String requestingUserId, Role requestingUserRole);

  /**
   * Performs a full replacement of an active experiment's (mutable) properties.
   * <p>
   * Archived experiments cannot be updated. The immutable properties
   * ({@code createdBy}, {@code archivedAt}) are protected during this operation.
   * </p>
   * The requesting user must be the creator, the designated scientist, or an admin.
   *
   * @param experimentId       The UUID of the experiment to update.
   * @param experimentDto      The new payload data.
   * @param requestingUserId   The internal ID of the authenticated user.
   * @param requestingUserRole The role of the authenticated user.
   * @return The updated {@link GetExperimentDto}.
   * @throws ResourceConflictException if the target experiment is archived.
   * @throws UnprocessableEntityException if any referenced chemical entities do not exist.
   * @throws AccessDeniedException if the user lacks ownership or admin rights.
   */
  GetExperimentDto updateExperiment(
    UUID experimentId,
    SaveExperimentDto experimentDto,
    String requestingUserId,
    Role requestingUserRole
  );

  /**
   * Executes a paginated search against the active experiment database.
   * <p>
   * Constructs a dynamic query based on the provided filters. Reactant A and B
   * are evaluated commutatively. Archived records are strictly excluded from the result set.
   * </p>
   *
   * @param searchParams       The object containing temporal, chemical, and measurement filters.
   * @param pageable           The pagination and sorting instructions.
   * @param requestingUserId   The internal ID of the authenticated user.
   * @param requestingUserRole The role of the authenticated user.
   * @return A {@link Page} containing the filtered {@link GetExperimentDto}s and pagination metadata.
   */
  Page<GetExperimentDto> searchExperiments(
    SearchParametersDto searchParams,
    Pageable pageable,
    String requestingUserId,
    Role requestingUserRole
  );
}
