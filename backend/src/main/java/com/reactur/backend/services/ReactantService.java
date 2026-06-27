package com.reactur.backend.services;

import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.ReactantDtos.CreateReactantDto;
import com.reactur.backend.dtos.ReactantDtos.UpdateReactantDto;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ReactantService {

    /**
     * Creates a new reactant in the system.
     *
     * @param saveReactant the DTO containing the data for the new reactant
     * @return the {@link ReactantDetailDto} representing the newly created reactant
     * @throws ResourceConflictException if a reactant with the same shortName already exists
     */
    ReactantDetailDto createReactant(CreateReactantDto saveReactant);

    /**
     * Updates an existing reactant with new details.
     *
     * @param uuid the unique identifier of the reactant to update
     * @param saveReactant the DTO containing the updated fields
     * @return the {@link ReactantDetailDto} of the updated reactant
     * @throws ResourceNotFoundException if no reactant is found for the given UUID
     * @throws ResourceConflictException if the updated shortName is already used by another reactant
     */
    ReactantDetailDto updateReactant(UUID uuid, UpdateReactantDto saveReactant);

    /**
     * Retrieves the comprehensive details of a specific reactant.
     *
     * @param uuid the unique identifier of the reactant
     * @return the {@link ReactantDetailDto} containing complete attributes
     * @throws ResourceNotFoundException if no reactant is found for the given UUID
     */
    ReactantDetailDto getReactantDetail(UUID uuid);

    /**
     * Retrieves all active (non-archived) reactants in the system.
     *
     * @return a List of {@link ReactantDto} containing basic reactant information
     */
    List<ReactantDto> getAllReactants();

    /**
     * Retrieves all archived reactants in the system.
     *
     * @return a List of {@link ReactantDto} containing basic information of archived reactants
     */
    List<ReactantDto> getArchivedReactants();

    /**
     * Archives a reactant by setting its archived timestamp.
     *
     * @param uuid the unique identifier of the reactant to archive
     * @throws ResourceNotFoundException if no reactant is found for the given UUID
     */
    void archiveReactant(UUID uuid);

    /**
     * Restores an archived reactant making it active again.
     *
     * @param uuid the unique identifier of the reactant to restore
     * @return the {@link ReactantDto} of the restored reactant
     * @throws ResourceNotFoundException if no reactant is found for the given UUID
     */
    ReactantDto restoreReactant(UUID uuid);
}
