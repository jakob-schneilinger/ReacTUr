package com.reactur.backend.dtos.experiments;

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
  @JsonProperty("pFO_rateConstant")
  Double pFO_rateConstant,

  @NotNull(message = "Rate constant is required")
  Double rateConstant,

  @NotNull(message = "Error value is required")
  @PositiveOrZero(message = "Error margin cannot be negative")
  Double error,

  @NotNull(message = "isErrorPercentage flag is required")
  Boolean isErrorPercentage
) {
}
