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

        @NotBlank(message = "Short name is a required field.")
        @Size(max = 100, message = "Short name must not exceed 100 characters.")
        String shortName,
        List<String> additionalShortNames,
        String chemicalName,
        String casNumber,
        String inChiKey,
        String comment,
        List<String> references
) {
}
