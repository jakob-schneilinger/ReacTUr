package com.reactur.backend.dtos.reactants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO used for creating or updating a reactant in the database.
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
public record CreateReactantDto(

        @NotBlank(message = "SMILES string must not be empty.")
        String smiles,

        @NotNull(message = "Chemical type must not be null.")
        UUID chemicalType,
        // (Semantic - Service Layer): Verify ChemicalType UUID exists in the database
        // (ReactantValidator#validateForCreate).

        @NotBlank(message = "Short name is a required field.")
        @Size(max = 100, message = "Short name must not exceed 100 characters.")
        String shortName,
        // (Semantic - Service Layer): Verify this short name isn't already used (case-insensitively)
        // as another reactant's shortName or additionalShortNames.

        // (Semantic - Service Layer): Verify no blank/duplicate (case-insensitive) entries, no overlap
        //  with shortName, and no conflict with any existing reactant's shortName/additionalShortNames
        //  (same check as shortName above, see ReactantValidator#validateForCreate/#validateAdditionalShortNames).
        List<String> additionalShortNames,
        String chemicalName,

        // (Semantic - Service Layer): If non-blank, verify no other reactant already uses this CAS number.
        String casNumber,

        // TODO (Semantic - Service Layer): Unlike casNumber above, inChiKey uniqueness is NOT verified on
        //  create today - ReactantValidator#validateForCreate never checks it, even though the update path
        //  (validateForUpdate) does via ReactantRepository#existsByInChiKeyAndIdNot, and a same-purpose
        //  ReactantRepository#existsByInChiKey(String) method already exists but is never called. This means
        //  two reactants can currently be created with the same non-blank inChiKey via POST, but the second one
        //  could never have its inChiKey set to that same value again via PATCH. Flagged as a currently-unenforced
        //  gap, not fixed here - see DTO validation review report.
        String inChiKey,
        String comment,
        List<String> references
) {
}
