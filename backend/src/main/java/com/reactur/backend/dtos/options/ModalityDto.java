package com.reactur.backend.dtos.options;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a measurement modality option managed by admins.
 *
 * @param uuid       The unique identifier of the modality.
 * @param name       The display name of the modality.
 * @param archivedAt The timestamp when the option was archived.
 */
public record ModalityDto(UUID uuid, @NotBlank(message = "Name must not be empty") String name, Instant archivedAt) implements OptionDto {
}
