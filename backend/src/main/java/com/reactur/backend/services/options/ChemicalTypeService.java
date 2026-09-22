package com.reactur.backend.services.options;

import com.reactur.backend.dtos.options.ChemicalTypeDto;

/** Concrete service contract for {@link ChemicalTypeDto} options, so Spring has a type to inject; all logic lives in {@link OptionService}. */
public interface ChemicalTypeService extends OptionService<ChemicalTypeDto> {
}
