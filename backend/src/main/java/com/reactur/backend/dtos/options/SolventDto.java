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
public record SolventDto(
  // TODO-CLAUDE (flagged, not fixed - needs a decision; see DTO validation review report): this DTO is
  //  dual-purpose (response shape for getKnownOptions() AND @RequestBody for createOptions()). On create,
  //  BaseMapper#toEntity maps this field directly onto the entity's @GeneratedValue(strategy = UUID) id
  //  BEFORE OptionServiceImpl#createOptions calls repository.save(...). A non-null id makes Spring Data
  //  JPA's isNew() check return false, so save() calls entityManager.merge() instead of persist(): a
  //  client-supplied uuid matching an existing option's id will silently overwrite that option's
  //  name/archivedAt instead of creating a new row, and even a novel client-chosen uuid bypasses
  //  server-side ID generation entirely. Confirmed via the generated SolventMapperImpl
  //  (solvent.id(dto.uuid())) - this value is NOT ignored on create.
  UUID uuid,

  @NotBlank(message = "Name must not be empty")
  String name,
  // (Semantic - Service Layer): Verify no existing option shares this name (case-insensitive)
  // - OptionServiceImpl#createOptions -> repository.existsByNameIgnoreCase.

  // TODO-CLAUDE: same createOptions() call also trusts this client-supplied value directly onto the
  //  entity (BaseMapper has no `@Mapping(target = "archivedAt", ignore = true)`), so a create request can
  //  set a brand-new option's archived state directly, or (combined with the uuid issue above) silently
  //  un-/re-archive an existing option. See DTO validation review report.
  Instant archivedAt
) implements OptionDto {
}
