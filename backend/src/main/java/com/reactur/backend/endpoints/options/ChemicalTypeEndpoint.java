package com.reactur.backend.endpoints.options;

import com.reactur.backend.dtos.options.ChemicalTypeDto;
import com.reactur.backend.services.options.ChemicalTypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Concrete REST controller for {@link ChemicalTypeDto} options; all endpoint logic lives in {@link AbstractOptionsEndpoint}. */
@RestController
@RequestMapping("/api/chemical_type")
public class ChemicalTypeEndpoint extends AbstractOptionsEndpoint<ChemicalTypeDto> {

    public ChemicalTypeEndpoint(ChemicalTypeService chemicalTypeService) {
        super(chemicalTypeService);
    }
}
