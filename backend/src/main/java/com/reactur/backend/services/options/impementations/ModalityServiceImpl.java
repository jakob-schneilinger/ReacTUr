package com.reactur.backend.services.options.impementations;

import com.reactur.backend.dtos.options.ModalityDto;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.mappers.options.ModalityMapper;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.services.options.ModalityService;
import org.springframework.stereotype.Service;

@Service
public class ModalityServiceImpl extends OptionServiceImpl<Modality, ModalityDto> implements ModalityService {

    public ModalityServiceImpl(ModalityRepository repository, ModalityMapper mapper) {
        super(repository, mapper);
    }
}
