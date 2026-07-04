package com.reactur.backend.dtos.reactants;

import com.reactur.backend.enums.StructureFlag;

import java.util.UUID;

/**
 * DTO representing a 3D structure associated with a reactant.
 *
 * @param id        The unique identifier of the structure.
 * @param reactant  The unique identifier of the associated reactant.
 * @param name      The name of the file (if uploaded) or an auto-generated identifier.
 * @param structure The actual 3D structure data (e.g., in XYZ format).
 * @param flag      The current lifecycle state of the structure (e.g., PENDING, ACTIVE).
 */
public record StructureDto(UUID id, UUID reactant, String name, String structure, StructureFlag flag) {
}
