package com.reactur.backend.services.options;

import com.reactur.backend.dtos.options.OptionDto;

import java.util.List;
import java.util.UUID;

public interface OptionService<T extends OptionDto> {

    List<T> getKnownOptions();

    List<T> createOptions(T optionDto);

    void archiveOption(UUID uuid);

    void restoreOption(UUID uuid);
}

