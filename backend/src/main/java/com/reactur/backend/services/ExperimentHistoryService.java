package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

public interface ExperimentHistoryService {

    /**
     * Retrieves the chronological audit trail for a specific experiment, including changes
     * to its metadata and results. Internal reference IDs are automatically resolved to
     * human-readable names.
     * <p>
     * Active experiments are accessible to all approved roles. If the experiment is archived,
     * the requesting user must be the creator, the designated scientist, or an admin.
     * </p>
     *
     * @param experimentUuid     the unique identifier of the experiment
     * @param requestingUserId   The internal ID of the authenticated user.
     * @param requestingUserRole The role of the authenticated user.
     * @return a List of {@link ExperimentHistoryDto} representing the history of changes, sorted newest to oldest
     * @throws ResourceNotFoundException if no experiment is found for the given UUID
     * @throws AccessDeniedException if attempting to access an unauthorized archived record.
     */
    List<ExperimentHistoryDto> getExperimentHistory(UUID experimentUuid, String requestingUserId, Role requestingUserRole);
}
