package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.GetMetadataDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.options.InstrumentDto;
import com.reactur.backend.dtos.options.ModalityDto;
import com.reactur.backend.dtos.options.SolventDto;
import com.reactur.backend.entities.experiment.*;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ReactantMapper.class, UserMapper.class})
public interface ExperimentMapper {

  // Main Experiment Mapping
  @Mapping(target = "uuid", source = "id")
  @Mapping(target = "measurementData", source = "result")
  @Mapping(target = "attachments", ignore = true)  // TODO? Handled later via dedicated attachment endpoint
  GetExperimentDto experimentToGetExperimentDto(Experiment experiment);

  // Nested Metadata Mapping
  @Mapping(target = "uuid", source = "id")
  GetMetadataDto metadataToGetMetadataDto(Metadata metadata);

  // Nested Result Mapping
  @Mapping(target = "pFO_rateConstant", source = "pfoRateConstant")
  @Mapping(target = "error", source = "errorValue")
  MeasurementDataDto resultToMeasurementDataDto(ExperimentResult result);

  @Mapping(source = "id", target = "uuid")
  SolventDto solventToSolventDto(Solvent solvent);

  @Mapping(source = "id", target = "uuid")
  InstrumentDto instrumentToInstrumentDto(Instrument instrument);

  @Mapping(source = "id", target = "uuid")
  ModalityDto modalityToModalityDto(Modality modality);
}

// TODO: Separate this into different mapper classes, as we also added here reactants and other staff that
//  shouldn't be here
