package com.reactur.backend.dtos.reactants;

import com.reactur.backend.dtos.options.ChemicalTypeDto;
import com.reactur.backend.enums.StructureFlag;

import java.util.UUID;

/**
 * DTO representing the basic information of a reactant.
 *
 * @param uuid         The unique identifier of the reactant.
 * @param smiles       The SMILES string representing the chemical structure.
 * @param chemicalType The categorization of the chemical.
 * @param shortName    The mandatory short name used for quick identification.
 * @param chemicalName The optional formal chemical name.
 * @param flag         Indicates if this reactant has an active structure.
 */
public record ReactantDto(
        UUID uuid,
        String smiles,
        ChemicalTypeDto chemicalType,
        String shortName,
        String chemicalName,
        StructureFlag flag
) {
}
