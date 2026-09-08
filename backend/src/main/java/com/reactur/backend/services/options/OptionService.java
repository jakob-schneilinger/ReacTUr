package com.reactur.backend.services.options;

import com.reactur.backend.dtos.options.OptionDto;
import com.reactur.backend.exceptions.ResourceConflictException;
import com.reactur.backend.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

public interface OptionService<T extends OptionDto> {

    /**
     * Retrieves all known (both active and archived) options of a specific type.
     *
     * @return a List of {@link T} representing all options of this type.
     */
    List<T> getKnownOptions();

    /**
     * Creates a new option in the system and returns the updated list of all options.
     *
     * @param optionDto the DTO containing the data for the new option.
     * @return the updated List of {@link T} containing all options (including the newly created one).
     * @throws ResourceConflictException if an option with the same name already exists.
     */
    List<T> createOptions(T optionDto);

    /**
     * Archives a specific option by setting its archived timestamp.
     *
     * @param uuid the unique identifier of the option to archive.
     * @throws ResourceNotFoundException if no option is found for the given UUID.
     */
    void archiveOption(UUID uuid);

    /**
     * Restores an archived option making it active again by clearing the archived timestamp.
     *
     * @param uuid the unique identifier of the option to restore.
     * @throws ResourceNotFoundException if no option is found for the given UUID.
     */
    void restoreOption(UUID uuid);
}

