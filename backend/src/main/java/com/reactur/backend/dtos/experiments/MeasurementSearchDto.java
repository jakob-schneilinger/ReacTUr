package com.reactur.backend.dtos.experiments;

/**
 * DTO for filtering experiments based on measurement value ranges.
 *
 * @param pFO_rateConstantMin The minimum pseudo-first-order rate constant.
 * @param pFO_rateConstantMax The maximum pseudo-first-order rate constant.
 * @param rateConstantMin     The minimum rate constant.
 * @param rateConstantMax     The maximum rate constant.
 */
public record MeasurementSearchDto(
  Double pFO_rateConstantMin,
  Double pFO_rateConstantMax,
  Double rateConstantMin,
  Double rateConstantMax

  // (Semantic - Service/DB Layer): Like the parent DTO, Min > Max inversions are allowed
  // in the payload and will simply result in 0 database matches.
) {
}
