package com.reactur.backend.mappers;

import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.entities.Reactant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

//TODO: Activate chemicalType when available

@Mapper(componentModel = "spring")
public interface ReactantMapper {

    @Mapping(source = "id", target = "uuid")
    @Mapping(target = "chemicalType", ignore = true)
    ReactantDetailDto toDetailDto(Reactant reactant);

    @Mapping(source = "id", target = "uuid")
    @Mapping(target = "chemicalType", ignore = true)
    ReactantDto toDto(Reactant reactant);
}
