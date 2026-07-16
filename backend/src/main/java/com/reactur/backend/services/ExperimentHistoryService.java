package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.exeptions.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ExperimentHistoryService {

    // TODO-CLAUDE (next thread): This method currently performs NO authorization check at all - any
    // authenticated user can view any experiment's history, including archived ones. Per team decision,
    // history should inherit ExperimentAuthorization.canView(...): active experiments -> any approved
    // user; archived -> creator/scientist/ADMIN/SUPERVISOR only. Needs requestingUserId/requestingUserRole
    // added here (and to ExperimentEndpoint#getAuditLogs), same pattern as ExperimentService/FileService.
    // Deliberately deferred in this pass, pending your review of the Javers-based implementation.
    /**
     * Retrieves the chronological audit trail for a specific experiment, including changes
     * to its metadata and results. Internal reference IDs are automatically resolved to
     * human-readable names.
     *
     * @param experimentUuid the unique identifier of the experiment
     * @return a List of {@link ExperimentHistoryDto} representing the history of changes, sorted newest to oldest
     * @throws ResourceNotFoundException if no experiment is found for the given UUID
     */
    List<ExperimentHistoryDto> getExperimentHistory(UUID experimentUuid);
}
