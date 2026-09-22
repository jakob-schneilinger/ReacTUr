package com.reactur.backend.services.options.implementations;

import com.reactur.backend.dtos.options.ChemicalTypeDto;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.mappers.options.ChemicalTypeMapper;
import com.reactur.backend.repositories.options.ChemicalTypeRepository;
import com.reactur.backend.services.options.ChemicalTypeService;
import org.springframework.stereotype.Service;

/** Concrete service bean for {@link ChemicalType} options; all logic lives in {@link OptionServiceImpl}. */
@Service
public class ChemicalTypeServiceImpl extends OptionServiceImpl<ChemicalType, ChemicalTypeDto> implements ChemicalTypeService {

    public ChemicalTypeServiceImpl(ChemicalTypeRepository repository, ChemicalTypeMapper mapper) {
        super(repository, mapper);
    }
}
