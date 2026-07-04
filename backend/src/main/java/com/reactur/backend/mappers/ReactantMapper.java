package com.reactur.backend.mappers;

import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.ReactantDtos.StructureDto;
import com.reactur.backend.dtos.SelectionOption.ChemicalTypeDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.enums.StructureFlag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReactantMapper {

    @Mapping(source = "reactant.id", target = "uuid")
    @Mapping(source = "reactant.type", target = "chemicalType")
    @Mapping(source = "structure", target = "structures")
    ReactantDetailDto toDetailDto(Reactant reactant, List<StructureDto> structure);

    @Mapping(source = "reactant.id", target = "uuid")
    @Mapping(source = "reactant.type", target = "chemicalType")
    ReactantDto toDto(Reactant reactant, StructureFlag flag);

    // TODO: Set a default Fallback, probably with a better flag than NULL
    // MapStruct Fallback: If no flag is provided, pass null.
    default ReactantDto toDto(Reactant reactant) {
        return toDto(reactant, null);
    }

    @Mapping(source = "id", target = "uuid")
    ChemicalTypeDto toChemicalTypeDto(ChemicalType chemicalType);
}
