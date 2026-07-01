package com.reactur.backend.client;

import java.util.List;

/**
 * Client interface for interacting with the external Svatunek-Lab Reactur API.
 * Handles the asynchronous job submission and status polling for 3D structure generation.
 */
public interface StructureApiClient {

    /**
     * Triggers a new job to generate 3D structures based on a SMILES string.
     *
     * @param smiles The SMILES string of the reactant.
     * @return The unique Job ID assigned by the external API.
     */
    String triggerJob(String smiles);

    /**
     * Polls the external API for the status of a specific job.
     *
     * @param jobId The unique Job ID received from {@link #triggerJob(String)}.
     * @return A {@link JobStatusResult} containing the current state ("running", "succeeded", etc.)
     * and the generated structures if the job is finished.
     */
    JobStatusResult checkJobStatus(String jobId);
}
