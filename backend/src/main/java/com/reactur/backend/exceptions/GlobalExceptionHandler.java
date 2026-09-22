package com.reactur.backend.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central, single place where every exception propagating out of an endpoint is caught, logged
 * exactly once, and turned into an HTTP response. Most handlers here are a direct, self-evident
 * mapping from one exception type to one status code; JavaDoc below is only added where the
 * choice of status code, log level, or response shape needs explaining.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Field-level validation failures from a {@code @Valid @RequestBody} DTO.
     *
     * @param ex the exception, carrying the per-field validation errors
     * @return a 400 response with a field-name-to-error-message map
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed for incoming request: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Constraint violations on a {@code @RequestParam}/{@code @PathVariable}, as opposed to a
     * validated request body (see {@link #handleValidationExceptions}).
     *
     * @param ex the exception
     * @return a 400 response with the violation message
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Logged at ERROR, not WARN: a storage failure is an infrastructure problem, not bad user input.
     *
     * @param ex the exception
     * @return a 500 response with a generic message
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<String> handleFileStorageException(FileStorageException ex) {
        log.error("File storage error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("A file storage error occurred. Please try again later.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request or missing request body: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Required request body is missing or malformed JSON.");
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<String> handleResourceConflictException(ResourceConflictException ex) {
        log.warn("Resource conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    /**
     * Thrown by Hibernate when a save or delete finds that the row was already changed by a
     * concurrent transaction since it was read. Mapped to 409, same as {@link
     * #handleResourceConflictException}, since it's the same kind of problem from the client's
     * point of view: retry with fresh data.
     *
     * @param ex the exception
     * @return a 409 response asking the client to reload and retry
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleObjectOptimisticLockingFailureException(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("This resource was modified by someone else in the meantime. Please reload and try again.");
    }

    /**
     * Semantic validation failures (e.g. a referenced entity doesn't exist), distinct from the
     * syntactic 400s above.
     *
     * @param ex the exception
     * @return a 422 response with a structured error body
     */
    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<Map<String, String>> handleUnprocessableEntityException(UnprocessableEntityException ex) {
        log.warn("Unprocessable Entity (Semantic Validation Failed): {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "Unprocessable Entity");
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(response);
    }

    /**
     * Both exception types mean the same thing to the caller (403 Forbidden) but come from two
     * different Spring Security mechanisms: {@code AccessDeniedException} from older
     * access-control checks, {@code AuthorizationDeniedException} from {@code @PreAuthorize}'s
     * newer authorization manager.
     *
     * @param ex      the exception
     * @param request the current request, used only for logging the method and path
     * @return a 403 response with a generic message
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<String> handleAccessDeniedException(Exception ex, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requestingUser = (authentication != null) ? authentication.getName() : "anonymous";
        log.warn("Access denied ({}) for user '{}' on {} {}",
                ex.getClass().getSimpleName(), requestingUser, request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Access Denied: You do not have the required permissions for this action.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(String.format("The requested HTTP method is not supported for this endpoint. Details: %s", ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("Endpoint or resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("The requested endpoint or resource was not found.");
    }

    /**
     * This project's own semantic-validation exception, distinct from {@link
     * #handleValidationExceptions}'s Spring/Jakarta one.
     *
     * @param ex the exception
     * @return a 422 response with the exception's message
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<String> handleValidationException(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ex.getMessage());
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<String> handleExternalApiException(ExternalApiException ex) {
        log.error("External API error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("The external service is currently unavailable or returned an error.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(String.format("The requested media type is not supported for this endpoint. Details: %s", ex.getMessage()));
    }

    /**
     * Special-cases a UUID-shaped path/query parameter with a friendlier message than the
     * generic type-mismatch case below.
     *
     * @param ex the exception
     * @return a 400 response with a message identifying the offending parameter
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());

        if (ex.getRequiredType() != null && ex.getRequiredType().isAssignableFrom(UUID.class)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(String.format("Invalid UUID format for parameter '%s'. Please provide a valid UUID.", ex.getName()));
        }

        String requiredTypeName = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(String.format("Parameter '%s' must be of type %s.", ex.getName(), requiredTypeName));
    }

    /**
     * Fallback for anything not caught by a more specific handler above; always unexpected, so
     * always logged at ERROR.
     *
     * @param ex the exception
     * @return a 500 response with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected server error occurred.");
    }
}