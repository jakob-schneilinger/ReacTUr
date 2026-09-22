package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.GetMetadataDto;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.mappers.options.InstrumentMapper;
import com.reactur.backend.mappers.options.ModalityMapper;
import com.reactur.backend.mappers.options.SolventMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps {@link Metadata} to {@link GetMetadataDto}, delegating nested scientist, solvent,
 * instrument, and modality conversion to the mappers named in {@code uses}.
 */
@Mapper(
  componentModel = "spring",
  uses = {
    UserMapper.class,
    SolventMapper.class,
    InstrumentMapper.class,
    ModalityMapper.class
  }
)
public interface MetadataMapper {
  @Mapping(target = "uuid", source = "id")
  GetMetadataDto metadataToGetMetadataDto(Metadata metadata);
}
