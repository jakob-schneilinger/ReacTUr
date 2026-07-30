package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.entities.experiment.ExperimentResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MeasurementDataMapperTest {

    private final MeasurementDataMapper mapper = Mappers.getMapper(MeasurementDataMapper.class);

    @Test
    void resultToMeasurementDataDto_shouldMapPfoRateConstantToPFO_andErrorValueToError() {
        ExperimentResult result = ExperimentResult.builder()
                .rateConstant(1.23)
                .pfoRateConstant(4.56)
                .errorValue(0.1)
                .isErrorPercentage(true)
                .build();

        MeasurementDataDto dto = mapper.resultToMeasurementDataDto(result);

        assertEquals(result.getPfoRateConstant(), dto.pFO_rateConstant());
        assertEquals(result.getRateConstant(), dto.rateConstant());
        assertEquals(result.getErrorValue(), dto.error());
        assertEquals(result.getIsErrorPercentage(), dto.isErrorPercentage());
    }

    @Test
    void resultToMeasurementDataDto_shouldMapNullPfoRateConstant() {
        ExperimentResult result = ExperimentResult.builder()
                .rateConstant(1.23)
                .pfoRateConstant(null)
                .errorValue(0.1)
                .isErrorPercentage(false)
                .build();

        MeasurementDataDto dto = mapper.resultToMeasurementDataDto(result);

        assertNull(dto.pFO_rateConstant());
    }
}
