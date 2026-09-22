package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.ExperimentAttachmentDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.entities.experiment.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Maps {@link Experiment} to {@link GetExperimentDto}, delegating nested reactant, user,
 * metadata, and measurement-result conversion to the mappers named in {@code uses}.
 */
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

  /**
   * Maps using the given, already-filtered attachment list. This is what production code paths
   * use, passing in attachments with archived ones already excluded.
   *
   * @param experiment  the entity to map
   * @param attachments the pre-filtered attachments to embed in the result
   * @return the mapped DTO
   */
  @Mapping(target = "uuid", source = "experiment.id")
  @Mapping(target = "measurementData", source = "experiment.result")
  @Mapping(target = "dois", source = "experiment.dois")
  @Mapping(target = "attachments", source = "attachments")
  GetExperimentDto experimentToGetExperimentDto(Experiment experiment, List<ExperimentAttachmentDto> attachments);

  /**
   * Maps the experiment, deriving attachments directly from {@code experiment.getFiles()} instead
   * of a separately-supplied list. Unlike the two-argument overload, this does not exclude
   * archived attachments.
   *
   * @param experiment the entity to map
   * @return the mapped DTO
   */
  @Mapping(target = "uuid", source = "id")
  @Mapping(target = "measurementData", source = "result")
  @Mapping(target = "attachments", source = "files")
  GetExperimentDto experimentToGetExperimentDto(Experiment experiment);

}
