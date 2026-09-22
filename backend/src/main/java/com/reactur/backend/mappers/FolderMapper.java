package com.reactur.backend.mappers;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentFolder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Maps {@link ExperimentFolder} to {@link GetFolderDto}, flattening its experiments down to just their IDs. */
@Mapper(componentModel = "spring")
public interface FolderMapper {

  @Mapping(target = "experimentIds", source = "experiments", qualifiedByName = "mapExperimentsToIds")
  GetFolderDto folderToGetFolderDto(ExperimentFolder folder);

  @Named("mapExperimentsToIds")
  default List<UUID> mapExperimentsToIds(List<Experiment> experiments) {
    if (experiments == null || experiments.isEmpty()) {
      return Collections.emptyList();
    }
    return experiments.stream()
      .map(Experiment::getId)
      .toList();
  }
}
