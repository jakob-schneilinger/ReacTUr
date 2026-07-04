package com.reactur.backend.services.options.impementations;

import com.reactur.backend.dtos.options.SolventDto;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.mappers.options.SolventMapper;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.services.options.SolventService;
import org.springframework.stereotype.Service;

@Service
public class SolventServiceImpl extends OptionServiceImpl<Solvent, SolventDto> implements SolventService {

    public SolventServiceImpl(SolventRepository repository, SolventMapper mapper) {
        super(repository, mapper);
    }
}
