package com.reactur.backend.exeptions;

import java.util.List;

/**
 * Thrown when request payload validation or business rule validation fails.
 * <p>
 * Handled globally to automatically return an HTTP {@code 400 Bad Request} status.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(List<String> reasons) {
        super("Validation failed:\n- " + String.join("\n- ", reasons));
    }
}
