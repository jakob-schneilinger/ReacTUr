package com.reactur.backend.client;

import com.reactur.backend.enums.JobStatus;

import java.util.List;

/**
 * A unified result object that our backend understands, abstracting away
 * the specific JSON structure of the external API.
 */
public record JobStatusResult(
        JobStatus status,
        List<String> structures,
        String errorMessage
) {
    public boolean isFinished() {
        return status != null && status.isFinished();
    }

    public boolean isFailed() {
        return (status != null && status.isFailed()) || errorMessage != null;
    }
}
