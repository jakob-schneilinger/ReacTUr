package com.reactur.backend.mappers.options;

import com.reactur.backend.dtos.options.ModalityDto;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.mappers.BaseMapper;
import org.mapstruct.Mapper;

/** Concrete mapper bean for {@link Modality} options; all mapping logic lives in {@link BaseMapper}. */
@Mapper(componentModel = "spring")
public interface ModalityMapper extends BaseMapper<Modality, ModalityDto> {
}
