package com.reactur.backend.dtos.ReactantDtos;

import com.reactur.backend.dtos.SelectionOption.ChemicalTypeDto;

import java.util.UUID;

/**
 * DTO representing the basic information of a reactant[cite: 6].
 *
 * @param uuid         The unique identifier of the reactant.
 * @param smiles       The SMILES string representing the chemical structure.
 * @param chemicalType The categorization of the chemical.
 * @param shortname    The mandatory short name used for quick identification.
 * @param chemicalName The optional formal chemical name.
 */
public record ReactantDto(
        UUID uuid,
        String smiles,
        ChemicalTypeDto chemicalType,
        String shortname,
        String chemicalName
) {
}
