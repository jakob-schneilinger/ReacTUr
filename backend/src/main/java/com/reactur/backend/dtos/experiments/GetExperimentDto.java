package com.reactur.backend.dtos.experiments;

import com.reactur.backend.dtos.DetailsDto;
import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.reactants.ReactantDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for retrieving the comprehensive details of an experiment.
 *
 * @param uuid            The unique identifier of the experiment.
 * @param doi             The Digital Object Identifier associated with the experiment.
 * @param reactantA       The first reactant involved in the reaction.
 * @param reactantB       The second reactant involved in the reaction.
 * @param metadata        The metadata associated with the experiment.
 * @param measurementData The main kinetics results (e.g., rate constants).
 * @param createdBy       The user who recorded the experiment in the system.
 * @param attachments     An array of files attached to this experiment.
 * @param archivedAt      The timestamp when the experiment was archived (null if active).
 */
public record GetExperimentDto(
        UUID uuid,
        String doi,
        ReactantDto reactantA,
        ReactantDto reactantB,
        GetMetadataDto metadata,
        MeasurementDataDto measurementData,
        GetUserDto createdBy,
        List<ExperimentAttachmentDto> attachments,
        Instant archivedAt
) implements DetailsDto {
}
