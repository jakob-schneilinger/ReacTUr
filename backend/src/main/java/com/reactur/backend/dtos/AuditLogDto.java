package com.reactur.backend.dtos;

import com.reactur.backend.enums.AuditAction;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a record of changes made within the system for tracking and security.
 *
 * @param uuid       The unique identifier of the audit log entry.
 * @param experiment The UUID or identifier of the affected experiment.
 * @param user       The user who performed the action.
 * @param timestamp  The exact time the action was performed.
 * @param action     The type of action performed (e.g., CREATE, UPDATE, DELETE).
 * @param changeFrom The state of the data before the change (usually JSON).
 * @param changeTo   The state of the data after the change (usually JSON).
 */
public record AuditLogDto(
        UUID uuid,
        String experiment,
        GetUserDto user,
        Instant timestamp,
        AuditAction action,
        String changeFrom,
        String changeTo
) {
}
