package com.reactur.backend.enums;

/**
 * Represents the lifecycle states of an asynchronous processing job.
 * Provide helper methods to evaluate the current execution phase.
 */
public enum JobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    UNKNOWN;

    /**
     * True only for {@link #SUCCEEDED}. A {@link #FAILED} job is finished too, but not "finished"
     * in this sense.
     *
     * @return true if this status is {@link #SUCCEEDED}
     */
    public boolean isFinished() {
        return this == SUCCEEDED;
    }

    public boolean isPending() {
        return this == QUEUED || this == RUNNING;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    /**
     * Parses the external API's status string, case-insensitively, falling back to {@link
     * #UNKNOWN} for anything unrecognized.
     *
     * @param status the raw status string from the external API
     * @return the parsed status
     */
    public static JobStatus fromString(String status) {
        if (status == null) return UNKNOWN;
        try {
            return JobStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}