package com.reactur.backend.services.options.implementations;

import com.reactur.backend.dtos.options.InstrumentDto;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.mappers.options.InstrumentMapper;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.services.options.InstrumentService;
import org.springframework.stereotype.Service;

/** Concrete service bean for {@link Instrument} options; all logic lives in {@link OptionServiceImpl}. */
@Service
public class InstrumentServiceImpl extends OptionServiceImpl<Instrument, InstrumentDto> implements InstrumentService {

    public InstrumentServiceImpl(InstrumentRepository repository, InstrumentMapper mapper) {
        super(repository, mapper);
    }
}
