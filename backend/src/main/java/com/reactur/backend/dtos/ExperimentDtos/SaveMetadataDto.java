package com.reactur.backend.dtos.ExperimentDtos;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating or updating experimental metadata.
 *
 * @param date           The date the experiment was conducted.
 * @param scientist      The UUID of the scientist responsible.
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
        LocalDate date,
        UUID scientist,
        Double temperature,
        UUID solvent,
        Double concentrationA,
        Double concentrationB,
        UUID instrument,
        UUID modality,
        Double wavelength,
        String comment
) {
}
