package com.reactur.backend.endpoints.options;

import com.reactur.backend.dtos.options.SolventDto;
import com.reactur.backend.services.options.SolventService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solvent")
public class SolventEndpoint extends AbstractOptionsEndpoint<SolventDto> {

    public SolventEndpoint(SolventService service) {
        super(service);
    }
}
