package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.ReactantStructure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps {@link ReactantStructure} to {@link StructureDto}. */
@Mapper(componentModel = "spring")
public interface StructureMapper {

    @Mapping(target = "reactant", source = "reactant.id")
    @Mapping(target = "name", source = "fileName")
    StructureDto toDto(ReactantStructure entity);
}
