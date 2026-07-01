package com.reactur.backend.services;

import com.reactur.backend.dtos.ReactantDtos.StructureDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface StructureService {

    /**
     * Uploads a structure file manually for a given reactant.
     *
     * @param reactantId The UUID of the reactant.
     * @param file The uploaded multipart file containing the structure.
     * @return The saved StructureDto.
     */
    StructureDto uploadStructure(UUID reactantId, MultipartFile file);

    /**
     * Triggers the generation of a 3D structure via the external API.
     * Creates a PENDING database entry with the external Job ID.
     *
     * @param reactantId The UUID of the reactant whose SMILES should be used.
     */
    void generateStructureFromAPI(UUID reactantId);

    /**
     * Selects a specific structure to be the active one.
     * This will set the flag of the selected structure to ACTIVE
     * and others of the same reactant to a REJECTED state.
     *
     * @param structureId The UUID of the structure to activate.
     * @return The updated StructureDto.
     */
    StructureDto selectStructure(UUID structureId);

    /**
     * Retrieves all structures associated with a specific reactant.
     * Useful for aggregating data in the ReactantDetailDto.
     *
     * @param reactantId The UUID of the reactant.
     * @return A list of StructureDto objects associated with the reactant.
     */
    List<StructureDto> getStructuresByReactant(UUID reactantId);

    /**
     * Scheduled method to poll the external API for PENDING jobs
     * and update the database with the generated structures once succeeded.
     */
    void fetchPendingStructuresFromAPI();
}
