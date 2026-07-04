package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.dtos.options.ChemicalTypeDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.enums.StructureFlag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

//TODO: Activate chemicalType when available

@Mapper(componentModel = "spring")
public interface ReactantMapper {

    @Mapping(source = "reactant.id", target = "uuid")
    @Mapping(target = "chemicalType", ignore = true)
    @Mapping(source = "structure", target = "structures")
    ReactantDetailDto toDetailDto(Reactant reactant, List<StructureDto> structure);

    @Mapping(source = "reactant.id", target = "uuid")
    @Mapping(target = "chemicalType", ignore = true)
    ReactantDto toDto(Reactant reactant, StructureFlag flag);

    @Mapping(source = "id", target = "uuid")
    @Mapping(target = "archivedAt", ignore = true) // TODO: Entity uses isActive, DTO expects archivedAt
    ChemicalTypeDto toChemicalTypeDto(ChemicalType chemicalType);
}
