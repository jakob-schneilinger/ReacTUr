package com.reactur.backend.dtos.SelectionOption;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a chemical type option managed by admins.
 *
 * @param uuid       The unique identifier of the chemical type.
 * @param name       The display name of the chemical type.
 * @param archivedAt The timestamp when the option was archived.
 */
public record ChemicalTypeDto(UUID uuid, String name, Instant archivedAt) implements SelectionOptionDto {
}
