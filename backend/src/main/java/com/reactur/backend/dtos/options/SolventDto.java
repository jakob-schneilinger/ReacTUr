package com.reactur.backend.dtos.options;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a solvent option managed by admins.
 *
 * @param uuid       The unique identifier of the solvent.
 * @param name       The display name of the solvent.
 * @param archivedAt The timestamp when the option was archived.
 */
public record SolventDto(UUID uuid, @NotBlank(message = "Name must not be empty") String name, Instant archivedAt) implements OptionDto {
}
