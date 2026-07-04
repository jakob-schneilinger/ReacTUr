package com.reactur.backend.dtos.options;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a measurement modality option managed by admins.
 *
 * @param uuid       The unique identifier of the modality.
 * @param name       The display name of the modality.
 * @param archivedAt The timestamp when the option was archived.
 */
public record ModalityDto(UUID uuid, String name, Instant archivedAt) implements OptionDto {
}
