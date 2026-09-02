package com.reactur.backend.dtos.experiments;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating or updating experimental metadata.
 *
 * @param date           The date the experiment was conducted.
 * @param temperature    The temperature during the experiment in °C.
 * @param solvent        The UUID of the solvent used.
 * @param concentrationA The concentration of reactant A.
 * @param concentrationB The concentration of reactant B.
 * @param instrument     The UUID of the instrument used.
 * @param modality       The UUID of the measurement modality.
 * @param wavelength     The wavelength used for measurement.
 * @param comment        Additional comments.
 */
public record SaveMetadataDto(
  @NotNull(message = "Date is required")
  @PastOrPresent(message = "Experiment date cannot be in the future")
  LocalDate date,

  @NotBlank(message = "Scientist ID is required")
  String scientist,  // (Semantic - Service Layer): Verify Scientist's exists in the database.

  @NotNull(message = "Temperature is required")
  @Min(value = -273, message = "Temperature cannot be below absolute zero") // Syntactic chemistry check
  Double temperature,

  @NotNull(message = "Solvent is required")
  UUID solvent, // (Semantic - Service Layer): Verify Solvent UUID exists in the database.

  @NotNull(message = "Concentration A is required")
  @PositiveOrZero(message = "Concentration cannot be negative")
  Double concentrationA,

  @NotNull(message = "Concentration B is required")
  @PositiveOrZero(message = "Concentration cannot be negative")
  Double concentrationB,

  UUID instrument, // (Semantic - Service Layer): Verify Instrument UUID exists in the database.

  UUID modality, // (Semantic - Service Layer): Verify Modality UUID exists in the database.

  @Positive(message = "Wavelength must be positive")
  Double wavelength,

  @Size(max = 1023, message = "Comment is too long") // Syntactic check to prevent massive text payloads
  String comment
) {
}
