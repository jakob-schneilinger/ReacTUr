package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.ExperimentAttachmentDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.entities.experiment.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

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
  @Mapping(target = "uuid", source = "experiment.id")
  @Mapping(target = "measurementData", source = "experiment.result")
  @Mapping(target = "attachments", source = "attachments")
  GetExperimentDto experimentToGetExperimentDto(Experiment experiment, List<ExperimentAttachmentDto> attachments);

}
