package com.reactur.backend.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code ObjectOptimisticLockingFailureException} handler added alongside the entities'
 * {@code @Version} fields (see the "Race condition inventory" item in Software_todo.md). Without this handler,
 * a genuine concurrent-write conflict (e.g. two admins editing the same reactant at once) would bubble up as an
 * unhandled 500 via the catch-all {@code handleGlobalException}, instead of the informative 409 a client can
 * reasonably react to (reload and retry).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleObjectOptimisticLockingFailureException_returns409WithRetryMessage() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("Reactant", "some-id");

        ResponseEntity<String> response = handler.handleObjectOptimisticLockingFailureException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("modified by someone else"));
    }
}
