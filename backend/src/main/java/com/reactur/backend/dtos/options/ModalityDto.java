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
public record ModalityDto(
  // Present for the response shape (getKnownOptions()).
  UUID uuid,

  @NotBlank(message = "Name must not be empty")
  String name,
  // (Semantic - Service Layer): Verify no existing option shares this name (case-insensitive)

  // Present for the response shape (getKnownOptions()); ignored on create — a newly created option
  // always starts non-archived regardless of what the client sends.
  Instant archivedAt
) implements OptionDto {
}
