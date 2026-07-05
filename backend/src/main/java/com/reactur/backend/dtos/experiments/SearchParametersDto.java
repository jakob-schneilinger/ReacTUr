package com.reactur.backend.dtos.experiments;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO encapsulating various filters to search for specific experiments across the platform.
 *
 * @param reactantA          Array of Reactant A UUIDs to include.
 * @param reactantB          Array of Reactant B UUIDs to include.
 * @param dateMin            The earliest experiment date.
 * @param dateMax            The latest experiment date.
 * @param scientist          Array of scientist UUIDs to include.
 * @param temperatureMin     The minimum temperature in °C.
 * @param temperatureMax     The maximum temperature in °C.
 * @param solvent            The UUID of the solvent to filter by.
 * @param concentrationA_Min The minimum concentration of reactant A.
 * @param concentrationA_Max The maximum concentration of reactant A.
 * @param concentrationB_Min The minimum concentration of reactant B.
 * @param concentrationB_Max The maximum concentration of reactant B.
 * @param instrument         The UUID of the instrument to filter by.
 * @param modality           The UUID of the modality to filter by.
 * @param wavelengthMin      The minimum wavelength.
 * @param wavelengthMax      The maximum wavelength.
 * @param comment            Text to search for within comments.
 * @param measurementSearch  Object of specific measurement range filters.
 */
public record SearchParametersDto(
        List<UUID> reactantA,
        List<UUID> reactantB,
        LocalDate dateMin,
        LocalDate dateMax,
        List<UUID> scientist,
        Double temperatureMin,
        Double temperatureMax,
        UUID solvent,
        Double concentrationA_Min,
        Double concentrationA_Max,
        Double concentrationB_Min,
        Double concentrationB_Max,
        UUID instrument,
        UUID modality,
        Double wavelengthMin,
        Double wavelengthMax,
        String comment,
        MeasurementSearchDto measurementSearch
) {
}
