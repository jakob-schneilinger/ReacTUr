package com.reactur.backend.services.options;

import com.reactur.backend.dtos.options.SolventDto;

/** Concrete service contract for {@link SolventDto} options, so Spring has a type to inject; all logic lives in {@link OptionService}. */
public interface SolventService extends OptionService<SolventDto> {
}
