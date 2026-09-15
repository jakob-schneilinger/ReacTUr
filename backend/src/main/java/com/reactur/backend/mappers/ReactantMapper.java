package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.enums.StructureFlag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReactantMapper {

    @Mapping(target = "uuid", source = "reactant.id")
    @Mapping(target = "chemicalType", source = "reactant.type")
    @Mapping(target = "structures", source = "structure")
    ReactantDetailDto toDetailDto(Reactant reactant, List<StructureDto> structure);

    @Mapping(target = "uuid", source = "reactant.id")
    @Mapping(target = "chemicalType", source = "reactant.type")
    // Explicit mapping needed: flag isn't a Reactant property, it's a value computed elsewhere
    // and passed in as this method's own second parameter, so MapStruct has no nested property
    // path to follow the way it does for reactant.id/reactant.type.
    @Mapping(target = "flag", source = "flag")
    ReactantDto toDto(Reactant reactant, StructureFlag flag);

    // Needed for mapping a Reactant with no computed flag available. Delegating with
    // flag=null is intentional, not a placeholder.
    default ReactantDto toDto(Reactant reactant) {
        return toDto(reactant, null);
    }
}
