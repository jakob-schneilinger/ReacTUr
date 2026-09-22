package com.reactur.backend.mappers.options;

import com.reactur.backend.dtos.options.ChemicalTypeDto;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.mappers.BaseMapper;
import org.mapstruct.Mapper;

/** Concrete mapper bean for {@link ChemicalType} options; all mapping logic lives in {@link BaseMapper}. */
@Mapper(componentModel = "spring")
public interface ChemicalTypeMapper extends BaseMapper<ChemicalType, ChemicalTypeDto> {
}
