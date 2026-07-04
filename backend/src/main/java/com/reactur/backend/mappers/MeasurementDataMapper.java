package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.entities.experiment.ExperimentResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MeasurementDataMapper {

  @Mapping(target = "pFO_rateConstant", source = "pfoRateConstant")
  @Mapping(target = "error", source = "errorValue")
  MeasurementDataDto resultToMeasurementDataDto(ExperimentResult result);
}
