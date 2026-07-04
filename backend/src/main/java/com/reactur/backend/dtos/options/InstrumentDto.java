package com.reactur.backend.dtos.options;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a measurement instrument option managed by admins.
 *
 * @param uuid       The unique identifier of the instrument.
 * @param name       The display name of the instrument.
 * @param archivedAt The timestamp when the option was archived.
 */
public record InstrumentDto(UUID uuid, @NotBlank(message = "Name must not be empty") String name, Instant archivedAt) implements OptionDto {
}
