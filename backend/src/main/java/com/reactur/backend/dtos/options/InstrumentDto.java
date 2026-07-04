package com.reactur.backend.dtos.options;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a measurement instrument option managed by admins.
 *
 * @param uuid       The unique identifier of the instrument.
 * @param name       The display name of the instrument.
 * @param archivedAt The timestamp when the option was archived.
 */
public record InstrumentDto(UUID uuid, String name, Instant archivedAt) implements OptionDto {
}
