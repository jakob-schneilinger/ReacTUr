package com.reactur.backend.dtos.options;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a chemical type option managed by admins.
 *
 * @param uuid       The unique identifier of the chemical type.
 * @param name       The display name of the chemical type.
 * @param archivedAt The timestamp when the option was archived.
 */
public record ChemicalTypeDto(
  // Present for the response shape (getKnownOptions()); ignored on create — see BaseMapper#toEntity.
  UUID uuid,

  @NotBlank(message = "Name must not be empty")
  String name,
  // (Semantic - Service Layer): Verify no existing option shares this name (case-insensitive)
  // - OptionServiceImpl#createOptions -> repository.existsByNameIgnoreCase.

  // Present for the response shape (getKnownOptions()); ignored on create — a newly created option
  // always starts non-archived regardless of what the client sends. See BaseMapper#toEntity.
  Instant archivedAt
) implements OptionDto {
}
