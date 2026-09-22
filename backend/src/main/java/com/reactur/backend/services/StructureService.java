package com.reactur.backend.services;

import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.exceptions.ExternalApiException;
import com.reactur.backend.exceptions.FileStorageException;
import com.reactur.backend.exceptions.ResourceConflictException;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Manages 3D structures for a reactant: manual upload, external generation, and selection.
 */
public interface StructureService {

    /**
     * Uploads a structure file manually for a given reactant.
     *
     * @param reactantId The UUID of the reactant.
     * @param file The uploaded multipart file containing the structure.
     * @return The saved StructureDto.
     * @throws IllegalArgumentException  if the file is null, empty, or exceeds the configured size limit.
     * @throws ResourceNotFoundException if no reactant exists for the given ID.
     * @throws FileStorageException      if the uploaded file can't be read.
     */
    StructureDto uploadStructure(UUID reactantId, MultipartFile file);

    /**
     * Triggers the generation of a 3D structure via the external API.
     * Creates a PENDING database entry with the external Job ID.
     *
     * @param reactantId The UUID of the reactant whose SMILES should be used.
     * @throws ResourceNotFoundException if no reactant exists for the given ID.
     * @throws ResourceConflictException if a structure generation is already in progress or
     *                                   awaiting selection, or a structure has already been
     *                                   generated and selected for this reactant.
     * @throws ExternalApiException      if the external structure-generation API is unreachable
     *                                   or returns an error.
     */
    void generateStructureFromAPI(UUID reactantId);

    /**
     * Selects a specific structure to be the active one.
     * This will set the flag of the selected structure to ACTIVE
     * and others of the same reactant to a REJECTED state.
     *
     * @param structureId The UUID of the structure to activate.
     * @return The updated StructureDto.
     * @throws ResourceNotFoundException if no structure exists for the given ID.
     * @throws ResourceConflictException if the structure is PENDING or FAILED and has no
     *                                   structure data available for selection.
     */
    StructureDto selectStructure(UUID structureId);

    /**
     * Retrieves all structures associated with a specific reactant.
     * Useful for aggregating data in the ReactantDetailDto.
     *
     * @param reactantId The UUID of the reactant.
     * @return A list of StructureDto objects associated with the reactant.
     * @throws ResourceNotFoundException if no reactant exists for the given ID.
     */
    List<StructureDto> getStructuresByReactant(UUID reactantId);

    /**
     * Scheduled method to poll the external API for PENDING jobs
     * and update the database with the generated structures once succeeded.
     */
    void fetchPendingStructuresFromAPI();
}
