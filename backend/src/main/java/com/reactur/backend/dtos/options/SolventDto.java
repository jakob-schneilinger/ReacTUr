package com.reactur.backend.dtos.options;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a solvent option managed by admins.
 *
 * @param uuid       The unique identifier of the solvent.
 * @param name       The display name of the solvent.
 * @param archivedAt The timestamp when the option was archived.
 */
public record SolventDto(UUID uuid, String name, Instant archivedAt) implements OptionDto {
}
