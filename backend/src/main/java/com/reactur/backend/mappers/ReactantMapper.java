package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.mappers.options.ChemicalTypeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Maps {@link Reactant} to its DTOs, delegating chemical-type conversion to {@link
 * ChemicalTypeMapper}.
 */
@Mapper(componentModel = "spring", uses = ChemicalTypeMapper.class)
public interface ReactantMapper {

    @Mapping(target = "uuid", source = "reactant.id")
    @Mapping(target = "chemicalType", source = "reactant.type")
    @Mapping(target = "structures", source = "structure")
    ReactantDetailDto toDetailDto(Reactant reactant, List<StructureDto> structure);

    /**
     * {@code flag} is mapped explicitly because it isn't a {@link Reactant} property: it's a
     * value computed elsewhere and passed in as this method's own parameter, so MapStruct has no
     * nested property path to follow the way it does for {@code reactant.id}/{@code reactant.type}.
     *
     * @param reactant the entity to map
     * @param flag     the reactant's computed aggregate structure flag
     * @return the mapped DTO
     */
    @Mapping(target = "uuid", source = "reactant.id")
    @Mapping(target = "chemicalType", source = "reactant.type")
    @Mapping(target = "flag", source = "flag")
    ReactantDto toDto(Reactant reactant, StructureFlag flag);

    /**
     * For mapping a reactant with no computed flag available. Delegating with a null flag is
     * intentional.
     *
     * @param reactant the entity to map
     * @return the mapped DTO, with a null flag
     */
    default ReactantDto toDto(Reactant reactant) {
        return toDto(reactant, null);
    }
}
