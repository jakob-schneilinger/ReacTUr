package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.entities.experiment.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
  componentModel = "spring",
  uses = {
    ReactantMapper.class,
    UserMapper.class,
    MetadataMapper.class,
    MeasurementDataMapper.class
  }
)
public interface ExperimentMapper {

  // Main Experiment Mapping
  @Mapping(target = "uuid", source = "id")
  @Mapping(target = "measurementData", source = "result")
  @Mapping(target = "attachments", ignore = true)  // TODO? Handled later via dedicated attachment endpoint
  GetExperimentDto experimentToGetExperimentDto(Experiment experiment);

}
