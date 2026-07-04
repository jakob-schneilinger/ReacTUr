package com.reactur.backend.mappers.options;

import com.reactur.backend.dtos.options.SolventDto;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.mappers.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SolventMapper extends BaseMapper<Solvent, SolventDto> {
}
