package com.reactur.backend.dtos.ReactantDtos;

import java.util.List;
import java.util.UUID;

/**
 * DTO used for creating or updating a reactant in the database.
 *
 * @param smiles               The SMILES string representing the chemical structure.
 * @param chemicalType         The UUID of the chemical type.
 * @param shortname            The mandatory short name.
 * @param additionalShortNames Array of other commonly used short names.
 * @param chemicalName         The formal chemical name.
 * @param casNumber            The CAS registry number.
 * @param inChiKey             The InChIKey for standardized structural identification.
 * @param comment              Additional notes about the reactant.
 * @param references           Array of literature or external references.
 */
public record SaveReactantDto(
        String smiles,
        UUID chemicalType,
        String shortname,
        List<String> additionalShortNames,
        String chemicalName,
        String casNumber,
        String inChiKey,
        String comment,
        List<String> references
) {
}
