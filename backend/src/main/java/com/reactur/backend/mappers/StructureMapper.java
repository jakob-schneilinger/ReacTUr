package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.ReactantStructure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StructureMapper {

    @Mapping(source = "reactant.id", target = "reactant")
    @Mapping(source = "fileName", target = "name")
    StructureDto toDto(ReactantStructure entity);
}
