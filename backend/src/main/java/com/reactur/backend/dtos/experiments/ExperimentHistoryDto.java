package com.reactur.backend.dtos.experiments;

import java.time.Instant;
import java.util.Map;

/**
 * DTO representing a historical record of changes made to an experiment.
 * Used to visualize the audit trail (Git-tree style) in the frontend.
 *
 * @param authorId   The ID of the user who performed the action.
 * @param commitDate The exact time the commit/change was recorded.
 * @param commitType The type of commit (e.g., "INITIAL_CREATE", "UPDATE", "ARCHIVE").
 * @param changes    A map containing the exact differences for this commit.
 */
public record ExperimentHistoryDto(
        String authorId,
        Instant commitDate,
        String commitType,
        Map<String, String> changes
         ) {
}
