package com.reactur.backend.dtos.experiments;

import com.reactur.backend.dtos.DetailsDto;
import com.reactur.backend.dtos.users.GetUserDto;
import com.reactur.backend.dtos.options.InstrumentDto;
import com.reactur.backend.dtos.options.ModalityDto;
import com.reactur.backend.dtos.options.SolventDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for retrieving the specific conditions and metadata of an experiment.
 *
 * @param uuid           The unique identifier of the metadata record.
 * @param date           The date the lab experiment was conducted.
 * @param scientist      The scientist responsible for the experiment.
 * @param temperature    The temperature during the experiment in °C.
 * @param solvent        The solvent used in the reaction.
 * @param concentrationA The concentration of reactant A.
 * @param concentrationB The concentration of reactant B.
 * @param instrument     The instrument used to measure the reaction.
 * @param modality       The modality of the measurement.
 * @param wavelength     The wavelength set during measurement.
 * @param comment        Additional user comments or notes.
 * @param archivedAt     The timestamp when this metadata was archived.
 */
public record GetMetadataDto(
        UUID uuid,
        LocalDate date,
        GetUserDto scientist,
        Double temperature,
        SolventDto solvent,
        Double concentrationA,
        Double concentrationB,
        InstrumentDto instrument,
        ModalityDto modality,
        Double wavelength,
        String comment,
        Instant archivedAt
) implements DetailsDto {
}
