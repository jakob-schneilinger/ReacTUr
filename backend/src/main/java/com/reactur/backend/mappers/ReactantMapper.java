package com.reactur.backend.mappers;

import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.SelectionOption.ChemicalTypeDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.SelectionOptions.ChemicalType;
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

    @Mapping(source = "id", target = "uuid")
    @Mapping(target = "archivedAt", ignore = true) // TODO: Entity uses isActive, DTO expects archivedAt
    ChemicalTypeDto toChemicalTypeDto(ChemicalType chemicalType);
}
