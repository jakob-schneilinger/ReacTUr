package com.reactur.backend.services.options;

import com.reactur.backend.dtos.options.ModalityDto;

/** Concrete service contract for {@link ModalityDto} options, so Spring has a type to inject; all logic lives in {@link OptionService}. */
public interface ModalityService extends OptionService<ModalityDto> {
}
