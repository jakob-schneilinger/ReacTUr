package com.reactur.backend.endpoints.options;

import com.reactur.backend.dtos.options.InstrumentDto;
import com.reactur.backend.services.options.InstrumentService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Concrete REST controller for {@link InstrumentDto} options; all endpoint logic lives in {@link AbstractOptionsEndpoint}. */
@RestController
@RequestMapping("/api/instrument")
public class InstrumentEndpoint extends AbstractOptionsEndpoint<InstrumentDto> {

    public InstrumentEndpoint(InstrumentService service) {
        super(service);
    }
}
