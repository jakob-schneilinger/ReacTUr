package com.reactur.backend.exceptions;

/**
 * Thrown when a requested database entity or API resource cannot be found.
 * <p>
 * Handled globally to automatically return an HTTP {@code 404 Not Found} status.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}