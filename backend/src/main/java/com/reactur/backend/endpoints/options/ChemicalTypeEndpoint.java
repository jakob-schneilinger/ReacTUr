package com.reactur.backend.endpoints.options;

import com.reactur.backend.dtos.options.ChemicalTypeDto;
import com.reactur.backend.services.options.ChemicalTypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chemical_type")
public class ChemicalTypeEndpoint extends AbstractOptionsEndpoint<ChemicalTypeDto> {

    public ChemicalTypeEndpoint(ChemicalTypeService chemicalTypeService) {
        super(chemicalTypeService);
    }
}
