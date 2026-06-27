package com.reactur.backend.exeptions;

/**
 * Thrown when a database entity or API resource conflict occurs (e.g., duplicate unique keys).
 * <p>
 * Handled globally to automatically return an HTTP {@code 409 Conflict} status.
 */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
}