package com.reactur.backend.dtos.ExperimentDtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO representing the main output kinetics results of an experiment.
 *
 * @param pFO_rateConstant  The pseudo-first-order rate constant (optional).
 * @param rateConstant      The primary rate constant result.
 * @param error             The error margin for the rate constant.
 * @param isErrorPercentage Indicates if the error is a percentage (true) or an absolute value (false).
 */
public record MeasurementDataDto(
  // TODO: Probably some more checks how the pFO should be verified, in case it's not null...
  @JsonProperty("pFO_rateConstant")
  Double pFO_rateConstant,

  // TODO: Some sanity checks for boundaries?
  @NotNull(message = "Rate constant is required")
  Double rateConstant,

  @NotNull(message = "Error value is required")
  @PositiveOrZero(message = "Error margin cannot be negative")
  Double error,

  @NotNull(message = "isErrorPercentage flag is required")
  Boolean isErrorPercentage

  // TODO (Semantic - Service Layer): If isErrorPercentage is true, we might need to verify that 'error'
  //  isn't wildly over 100%.
) {
}
