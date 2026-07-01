package com.reactur.backend.mappers;

import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.ReactantDtos.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.enums.StructureFlag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

//TODO: Activate chemicalType when available

@Mapper(componentModel = "spring")
public interface ReactantMapper {

    @Mapping(source = "reactant.id", target = "uuid")
    @Mapping(target = "chemicalType", ignore = true)
    ReactantDetailDto toDetailDto(Reactant reactant, List<StructureDto> structure);

    @Mapping(source = "reactant.id", target = "uuid")
    @Mapping(target = "chemicalType", ignore = true)
    ReactantDto toDto(Reactant reactant, StructureFlag flag);
}
