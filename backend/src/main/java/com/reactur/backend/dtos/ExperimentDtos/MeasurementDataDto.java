package com.reactur.backend.dtos.ExperimentDtos;

/**
 * DTO representing the main output kinetics results of an experiment.
 *
 * @param pFO_rateConstant  The pseudo-first-order rate constant (optional).
 * @param rateConstant      The primary rate constant result.
 * @param error             The error margin for the rate constant.
 * @param isErrorPercentage Indicates if the error is a percentage (true) or an absolute value (false).
 */
public record MeasurementDataDto(
        Double pFO_rateConstant,
        Double rateConstant,
        Double error,
        Boolean isErrorPercentage
) {
}
