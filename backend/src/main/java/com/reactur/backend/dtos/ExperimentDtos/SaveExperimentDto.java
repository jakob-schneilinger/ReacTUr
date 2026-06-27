package com.reactur.backend.dtos.ExperimentDtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO for creating or updating an experiment record.
 *
 * @param reactantA       The UUID or identifier of the first reactant.
 * @param reactantB       The UUID or identifier of the second reactant.
 * @param metadata        The experimental setup and conditions to save.
 * @param measurementData The kinetics results to save.
 * @param doi             The Digital Object Identifier (optional).
 */
public record SaveExperimentDto(

  // TODO: Change back where scientist was here

  @NotNull(message = "Reactant A is required")
  UUID reactantA,  // (Semantic - Service Layer): Verify this UUID actually exists in the Reactant DB table.

  @NotNull(message = "Reactant B is required")
  UUID reactantB,  // (Semantic - Service Layer): Verify this UUID actually exists in the Reactant DB table.

  @Valid
  @NotNull(message = "Metadata is required")
  SaveMetadataDto metadata,

  @Valid
  @NotNull(message = "Measurement data is required")
  MeasurementDataDto measurementData,

  // TODO (Syntactic - DTO): If DOIs follow a strict format,
  //  add a @Pattern(regexp = "^10\\.\\d{4,9}/[-._;()/:A-Z0-9]+$") here.
  String doi
) {
}
