package com.reactur.backend.mappers.options;

import com.reactur.backend.dtos.options.InstrumentDto;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.mappers.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstrumentMapper extends BaseMapper<Instrument, InstrumentDto> {
}
