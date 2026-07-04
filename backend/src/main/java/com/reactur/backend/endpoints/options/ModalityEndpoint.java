package com.reactur.backend.endpoints.options;

import com.reactur.backend.dtos.options.ModalityDto;
import com.reactur.backend.services.options.ModalityService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modality")
public class ModalityEndpoint extends AbstractOptionsEndpoint<ModalityDto> {

    public ModalityEndpoint(ModalityService service) {
        super(service);
    }
}
