package com.reactur.backend.services.options;

import com.reactur.backend.dtos.options.InstrumentDto;

/** Concrete service contract for {@link InstrumentDto} options, so Spring has a type to inject; all logic lives in {@link OptionService}. */
public interface InstrumentService extends OptionService<InstrumentDto> {
}
