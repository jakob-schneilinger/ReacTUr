package com.reactur.backend.dtos.ReactantDtos;

import com.reactur.backend.enums.StructureFlag;

import java.util.UUID;

/**
 *
 * @param id
 * @param reactant
 * @param name
 * @param structure
 * @param flag
 */
public record StructureDto(UUID id, UUID reactant, String name, String structure, StructureFlag flag) {
}
