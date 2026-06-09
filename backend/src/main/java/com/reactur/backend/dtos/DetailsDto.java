package com.reactur.backend.dtos;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for DTOs that represent detailed entities capable of being soft-deleted (archived).
 */
public interface DetailsDto {

    /**
     * Gets the unique identifier of the entity.
     *
     * @return The UUID of the entity.
     */
    UUID uuid();

    /**
     * Gets the timestamp indicating when the entity was archived.
     *
     * @return The archive timestamp, or null if the entity is active.
     */
    Instant archivedAt();
}
