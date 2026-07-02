package com.reactur.backend.exeptions;

/**
 * Thrown when a request payload is syntactically correct but semantically invalid
 * (for example referencing a UUID foreign key that does not exist in the database).
 * <p>
 * Handled globally to return an HTTP {@code 422 Unprocessable Entity}.
 */
public class UnprocessableEntityException extends RuntimeException {
  public UnprocessableEntityException(String message) {
    super(message);
  }
}
