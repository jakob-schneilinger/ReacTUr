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
    MeasurementDataMapper.class,
    ExperimentAttachmentMapper.class
  }
)
public interface ExperimentMapper {

  @Mapping(target = "uuid", source = "experiment.id")
  @Mapping(target = "measurementData", source = "experiment.result")
  @Mapping(target = "dois", source = "experiment.dois")
  @Mapping(target = "attachments", source = "attachments")
  GetExperimentDto experimentToGetExperimentDto(Experiment experiment, List<ExperimentAttachmentDto> attachments);

  @Mapping(target = "uuid", source = "id")
  @Mapping(target = "measurementData", source = "result")
  @Mapping(target = "attachments", source = "files")
  GetExperimentDto experimentToGetExperimentDto(Experiment experiment);

}
