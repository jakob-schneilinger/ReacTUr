package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.enums.StructureFlag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

//TODO: Activate chemicalType when available

@Mapper(componentModel = "spring")
public interface ReactantMapper {

    @Mapping(target = "uuid", source = "reactant.id")
    @Mapping(target = "chemicalType", source = "reactant.type")
    @Mapping(target = "structures", source = "structure")
    ReactantDetailDto toDetailDto(Reactant reactant, List<StructureDto> structure);

    @Mapping(target = "uuid", source = "reactant.id")
    @Mapping(target = "chemicalType", source = "reactant.type")
    @Mapping(target = "flag", source = "flag") // TODO: Sync with Frontend: We have to add this explicit mapping!
    ReactantDto toDto(Reactant reactant, StructureFlag flag);

    // RESTORED: MapStruct needs this to know how to map a Reactant with no flag provided
    default ReactantDto toDto(Reactant reactant) {
        return toDto(reactant, null);  // TODO: Chose a good default value, or some Default Flag
    }
}
