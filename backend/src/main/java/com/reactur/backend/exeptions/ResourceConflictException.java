package com.reactur.backend.exeptions;

import java.util.List;

/**
 * Thrown when a database entity or API resource conflict occurs (e.g., duplicate unique keys).
 * <p>
 * Handled globally to automatically return an HTTP {@code 409 Conflict} status.
 */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }

    public ResourceConflictException(List<String> reasons) {
        super("Resource conflict detected:\n- " + String.join("\n- ", reasons));
    }
}