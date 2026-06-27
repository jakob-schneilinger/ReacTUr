package com.reactur.backend.mappers;

import com.reactur.backend.dtos.ExperimentDtos.*;
import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.entities.ExperimentEntities.*;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

                                   //  Tells our MapStruct Framework to make this
@Mapper(componentModel = "spring") // a Spring Bean we can inject!
public interface ExperimentMapper {

  // Main Experiment Mapping
  @Mapping(target = "uuid", source = "id")
  @Mapping(target = "measurementData", source = "result")
  @Mapping(target = "attachments", ignore = true)  // TODO: correctly set when we have a mapper for attachments (Gemini: Handled later via dedicated attachment endpoint)
  GetExperimentDto experimentToGetExperimentDto(Experiment experiment);

  // Nested Metadata Mapping
  @Mapping(target = "uuid", source = "id")
  @Mapping(target = "solvent", ignore = true)    // TODO: Map to SolventDto once options are refined
  @Mapping(target = "instrument", ignore = true) // TODO: Map to InstrumentDto once options are refined
  @Mapping(target = "modality", ignore = true)   // TODO: Map to ModalityDto once options are refined
  GetMetadataDto metadataToGetMetadataDto(Metadata metadata);

  // Nested Result Mapping
  @Mapping(target = "pFO_rateConstant", source = "pfoRateConstant")
  @Mapping(target = "error", source = "errorValue")
  MeasurementDataDto resultToMeasurementDataDto(ExperimentResult result);

  // The Reactant Mapping
  @Mapping(target = "uuid", source = "id")
  @Mapping(target = "chemicalType", source="type")
  ReactantDto reactantToReactantDto(Reactant reactant);

  // User Mapping
  @Mapping(target = "deactivatedAt")
  GetUserDto userToGetUserDto(User user);
}

// TODO: Separate this into different mapper classes, as we also added here reactants and other staff that
//  shouldn't be here
