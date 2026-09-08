package com.reactur.backend.exceptions;

/**
 * Thrown when an error occurs while communicating with an external third-party service.
 * <p>
 * Handled globally to return an HTTP {@code 502 Bad Gateway}.
 */
public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
