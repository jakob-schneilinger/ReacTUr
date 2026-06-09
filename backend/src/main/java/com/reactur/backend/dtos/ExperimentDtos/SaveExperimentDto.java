package com.reactur.backend.dtos.ExperimentDtos;

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
        String reactantA,
        String reactantB,
        SaveMetadataDto metadata,
        MeasurementDataDto measurementData,
        String doi
) {
}
