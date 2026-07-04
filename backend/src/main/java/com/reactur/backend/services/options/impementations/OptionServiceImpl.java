package com.reactur.backend.services.options.impementations;

import com.reactur.backend.dtos.options.OptionDto;
import com.reactur.backend.entities.options.BaseOption;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.BaseMapper;
import com.reactur.backend.repositories.options.OptionRepository;
import com.reactur.backend.services.options.OptionService;

import java.time.Instant;
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
        List<E> entities = repository.findAll();
        return mapper.toDtoList(entities);
    }

    @Override
    public List<T> createOptions(T optionDto) {
        if (repository.existsByNameIgnoreCase(optionDto.name())) {
            throw new ResourceConflictException("An option with the name '" + optionDto.name() + "' already exists.");
        }

        E entity = mapper.toEntity(optionDto);
        repository.save(entity);

        return getKnownOptions();
    }

    @Override
    public void archiveOption(UUID uuid) {
        E entity = repository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Option with ID " + uuid + " not found."));

        entity.setArchivedAt(Instant.now());
        repository.save(entity);
    }

    @Override
    public void restoreOption(UUID uuid) {
        E entity = repository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Option with ID " + uuid + " not found."));

        entity.setArchivedAt(null);
        repository.save(entity);
    }
}
