package com.reactur.backend.dtos.experiments;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating or updating an experiment record.
 *
 * @param reactantA       The UUID or identifier of the first reactant.
 * @param reactantB       The UUID or identifier of the second reactant.
 * @param metadata        The experimental setup and conditions to save.
 * @param measurementData The kinetics results to save.
 * @param dois            The Digital Object Identifiers (optional).
 */
public record SaveExperimentDto(

  @NotNull(message = "Reactant A is required")
  UUID reactantA,
  // (Semantic - Service Layer): Verify this UUID exists in the Reactant DB table, differs from reactantB,
  // and has an ACTIVE 3D structure (see ExperimentValidator#validate).

  @NotNull(message = "Reactant B is required")
  UUID reactantB,
  // (Semantic - Service Layer): Verify this UUID exists in the Reactant DB table, differs from reactantA,
  // and has an ACTIVE 3D structure (see ExperimentValidator#validate).

  @Valid
  @NotNull(message = "Metadata is required")
  SaveMetadataDto metadata,

  @Valid
  @NotNull(message = "Measurement data is required")
  MeasurementDataDto measurementData,

  // TODO (Syntactic - DTO): Ask if DOIs follow a strict format,
  //  add a @Pattern(regexp = "^10\\.\\d{4,9}/[-._;()/:A-Z0-9]+$") here.
  List<String> dois
) {
  // Guarantees that dois is never null, but rather an empty list
  public SaveExperimentDto {
    if (dois == null) {
      dois = Collections.emptyList();
    }
  }
}
