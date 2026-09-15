package com.reactur.backend.dtos.reactants;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO used for updating a reactant in the database.
 *
 * @param smiles               The SMILES string representing the chemical structure.
 * @param chemicalType         The UUID of the chemical type.
 * @param shortName            The mandatory short name.
 * @param additionalShortNames Array of other commonly used short names.
 * @param chemicalName         The formal chemical name.
 * @param casNumber            The CAS registry number.
 * @param inChiKey             The InChIKey for standardized structural identification.
 * @param comment              Additional notes about the reactant.
 * @param references           Array of literature or external references.
 */
public record UpdateReactantDto(

        // (Semantic - Service Layer): If provided (non-null), must not be blank/whitespace-only. Rejected
        //  with a ValidationException.
        //  Deliberately not a Jakarta
        //  @NotBlank, since that would also reject omitting this field entirely, breaking the PATCH.
        String smiles,

        // (Semantic - Service Layer): If provided (non-null), verify ChemicalType UUID exists in the database
        UUID chemicalType,

        // (Semantic - Service Layer): If provided (non-null), must not be blank/whitespace-only (same
        //  PATCH-aware exception as smiles above), and must not conflict (case-insensitively) with another
        //  reactant's shortName/additionalShortNames.
        @Size(max = 100, message = "Short name must not exceed 100 characters.")
        String shortName,

        // (Semantic - Service Layer): If provided (non-null), verify no blank/duplicate (case-insensitive)
        //  entries, no overlap with the effective shortName, and no conflict with any other reactant's
        //  shortName/additionalShortNames.
        List<String> additionalShortNames,
        String chemicalName,

        // (Semantic - Service Layer): If provided (non-null), changed, and non-blank, verify no other
        //  reactant already uses this CAS number.
        String casNumber,

        // (Semantic - Service Layer): If provided (non-null), changed, and non-blank, verify no other
        //  reactant already uses this InChIKey.
        String inChiKey,
        String comment,
        List<String> references
) {
}
