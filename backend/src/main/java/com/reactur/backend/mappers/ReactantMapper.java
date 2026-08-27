package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.enums.StructureFlag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReactantMapper {

    @Mapping(target = "uuid", source = "reactant.id")
    @Mapping(target = "chemicalType", source = "reactant.type")
    @Mapping(target = "structures", source = "structure")
    ReactantDetailDto toDetailDto(Reactant reactant, List<StructureDto> structure);

    @Mapping(target = "uuid", source = "reactant.id")
    @Mapping(target = "chemicalType", source = "reactant.type")
    // Explicit mapping needed here (unlike uuid/chemicalType above): "flag" isn't a Reactant
    // property, it's the aggregate structure state computed by the service layer (see
    // ReactantServiceImpl.determineAggregateFlag) and passed in as this method's own second
    // parameter. Without naming the parameter itself via "source = flag", MapStruct has no
    // nested-property path to follow the way it does for reactant.id/reactant.type.
    // Frontend consistency confirmed directly (frontend-ang/src/app/features/reactants): the
    // ReactantDto.flag field is already typed optional there, and reactant-overview-page's own
    // sort-rank function explicitly types it as "StructureFlag | null | undefined" with a
    // deliberate fallback branch - a missing/null flag was already anticipated and handled, not
    // an open question.
    @Mapping(target = "flag", source = "flag")
    ReactantDto toDto(Reactant reactant, StructureFlag flag);

    // MapStruct needs this overload for a mapper usable with just a Reactant (no computed flag
    // available/needed) - it was removed once and had to be restored, so don't delete it again
    // without checking why it was added. Delegating with flag=null is intentional, not a
    // placeholder guess: every real call site of the two-arg overload above
    // (ReactantServiceImpl.getAllReactants/getArchivedReactants/restoreReactant) already treats
    // null as "this reactant has no structures" (see determineAggregateFlag's own null-returning
    // branch), so this default is already consistent with real behavior throughout the codebase -
    // not a separate special case needing its own "default flag" value.
    default ReactantDto toDto(Reactant reactant) {
        return toDto(reactant, null);
    }
}
