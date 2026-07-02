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

    public boolean isFinished() {
        return this == SUCCEEDED;
    }

    public boolean isPending() {
        return this == QUEUED || this == RUNNING;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public static JobStatus fromString(String status) {
        if (status == null) return UNKNOWN;
        try {
            return JobStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}