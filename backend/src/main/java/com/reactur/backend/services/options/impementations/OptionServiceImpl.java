package com.reactur.backend.services.options.impementations;

import com.reactur.backend.dtos.options.OptionDto;
import com.reactur.backend.entities.options.BaseOption;
import com.reactur.backend.mappers.BaseMapper;
import com.reactur.backend.repositories.options.OptionRepository;
import com.reactur.backend.services.options.OptionService;

import java.util.List;
import java.util.UUID;

public abstract class OptionServiceImpl<E extends BaseOption, T extends OptionDto> implements OptionService<T> {

    protected final OptionRepository<E> repository;
    protected final BaseMapper<E, T> mapper;

    protected OptionServiceImpl(OptionRepository<E> repository, BaseMapper<E, T> mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<T> getKnownOptions() {
        throw new UnsupportedOperationException("TDD: Needs implementation");
    }

    @Override
    public List<T> createOptions(T optionDto) {
        throw new UnsupportedOperationException("TDD: Needs implementation");
    }

    @Override
    public void archiveOption(UUID uuid) {
        throw new UnsupportedOperationException("TDD: Needs implementation");
    }

    @Override
    public void restoreOption(UUID uuid) {
        throw new UnsupportedOperationException("TDD: Needs implementation");
    }
}
