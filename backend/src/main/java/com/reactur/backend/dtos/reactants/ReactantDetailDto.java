package com.reactur.backend.dtos.reactants;

import com.reactur.backend.dtos.DetailsDto;
import com.reactur.backend.dtos.options.ChemicalTypeDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing the comprehensive details of a reactant.
 *
 * @param uuid                 The unique identifier of the reactant.
 * @param smiles               The SMILES string representing the chemical structure.
 * @param chemicalType         The categorization of the chemical.
 * @param shortName            The mandatory short name.
 * @param additionalShortNames Array of other commonly used short names.
 * @param chemicalName         The formal chemical name.
 * @param casNumber            The CAS registry number.
 * @param inChiKey             The InChIKey for standardized structural identification.
 * @param comment              Additional notes about the reactant.
 * @param references           Array of literature or external references.
 * @param archivedAt           The timestamp when the reactant was archived (null if active).
 * @param structures            List of structures associated with this reactant.
 */
public record ReactantDetailDto(
        UUID uuid,
        String smiles,
        ChemicalTypeDto chemicalType,
        String shortName,
        List<String> additionalShortNames,
        String chemicalName,
        String casNumber,
        String inChiKey,
        String comment,
        List<String> references,
        Instant archivedAt,
        List<StructureDto> structures
) implements DetailsDto {
}
