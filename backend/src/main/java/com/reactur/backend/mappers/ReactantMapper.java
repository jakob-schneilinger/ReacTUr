package com.reactur.backend.mappers;

import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.entities.Reactant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReactantMapper {

    @Mapping(source = "id", target = "uuid")
    @Mapping(source = "type", target = "chemicalType")
    ReactantDetailDto toDetailDto(Reactant reactant);

    @Mapping(source = "id", target = "uuid")
    @Mapping(source = "type", target = "chemicalType")
    ReactantDto toDto(Reactant reactant);
}
