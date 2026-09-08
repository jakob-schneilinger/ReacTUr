package com.reactur.backend.exceptions;

/**
 * Thrown when an error occurs during file storage, retrieval, or processing operations.
 * <p>
 * Handled globally to return an HTTP {@code 500 Internal Server Error}.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
