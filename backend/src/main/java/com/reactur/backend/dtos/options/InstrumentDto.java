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
public record InstrumentDto(
  // Present in the response only; ignored on create.
  UUID uuid,

  @NotBlank(message = "Name must not be empty")
  String name,
  // (Semantic - Service Layer): Verify no existing option shares this name (case-insensitive)

  // Present in the response only; ignored on create. A newly created option always starts
  // non-archived regardless of what the client sends.
  Instant archivedAt
) implements OptionDto {
}
