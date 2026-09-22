package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.mappers.options.ChemicalTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the field mappings this mapper produces, including that the nested chemical type is
 * correctly delegated to {@link ChemicalTypeMapper} and that the computed structure flag passes
 * through both {@code toDto} overloads correctly.
 */
class ReactantMapperTest {

    private final ReactantMapper mapper = Mappers.getMapper(ReactantMapper.class);

    private Reactant reactant;
    private ChemicalType chemicalType;

    @BeforeEach
    void setUp() {
        // ReactantMapperImpl relies on Spring field injection for its `uses` mapper, so for a plain
        // unit test (no Spring context) we wire it in manually.
        ReflectionTestUtils.setField(mapper, "chemicalTypeMapper", Mappers.getMapper(ChemicalTypeMapper.class));

        chemicalType = ChemicalType.builder()
                .id(UUID.randomUUID())
                .name("Acid")
                .build();

        reactant = Reactant.builder()
                .id(UUID.randomUUID())
                .shortName("HCl")
                .smiles("Cl")
                .type(chemicalType)
                .chemicalName("Hydrochloric acid")
                .casNumber("7647-01-0")
                .inChiKey("VEXZGXHMUGYJMC-UHFFFAOYSA-N")
                .comment("test comment")
                .additionalShortNames(List.of("Muriatic acid"))
                .references(List.of("doi:10.1000/xyz"))
                .archivedAt(null)
                .build();
    }

    @Test
    void toDetailDto_shouldMapReactantIdToUuid_andIncludeProvidedStructures() {
        StructureDto structure = new StructureDto(UUID.randomUUID(), reactant.getId(), "structure.xyz", "data", StructureFlag.ACTIVE);

        ReactantDetailDto dto = mapper.toDetailDto(reactant, List.of(structure));

        assertEquals(reactant.getId(), dto.uuid());
        assertEquals(reactant.getSmiles(), dto.smiles());
        assertEquals(reactant.getShortName(), dto.shortName());
        assertEquals(reactant.getChemicalName(), dto.chemicalName());
        assertEquals(reactant.getCasNumber(), dto.casNumber());
        assertEquals(reactant.getInChiKey(), dto.inChiKey());
        assertEquals(reactant.getComment(), dto.comment());
        assertEquals(reactant.getAdditionalShortNames(), dto.additionalShortNames());
        assertEquals(reactant.getReferences(), dto.references());
        assertEquals(List.of(structure), dto.structures());

        assertEquals(chemicalType.getName(), dto.chemicalType().name());
        assertEquals(chemicalType.getId(), dto.chemicalType().uuid());
    }

    @Test
    void toDetailDto_shouldMapNullStructures_whenStructuresListIsNull() {
        ReactantDetailDto dto = mapper.toDetailDto(reactant, null);

        assertNull(dto.structures());
    }

    @Test
    void toDetailDto_shouldMapEmptyStructures_whenStructuresListIsEmpty() {
        ReactantDetailDto dto = mapper.toDetailDto(reactant, List.of());

        assertEquals(List.of(), dto.structures());
    }

    @Test
    void toDto_withFlag_shouldMapReactantIdToUuid_andPassThroughFlag() {
        ReactantDto dto = mapper.toDto(reactant, StructureFlag.READY);

        assertEquals(reactant.getId(), dto.uuid());
        assertEquals(reactant.getSmiles(), dto.smiles());
        assertEquals(reactant.getShortName(), dto.shortName());
        assertEquals(reactant.getChemicalName(), dto.chemicalName());
        assertEquals(StructureFlag.READY, dto.flag());
        assertEquals(chemicalType.getName(), dto.chemicalType().name());
        assertEquals(chemicalType.getId(), dto.chemicalType().uuid());
    }

    @Test
    void toDto_withoutFlag_shouldDelegateWithNullFlag() {
        ReactantDto dto = mapper.toDto(reactant);

        assertEquals(reactant.getId(), dto.uuid());
        assertNull(dto.flag());
    }

    @Test
    void toDto_withExplicitNullFlag_shouldMapNullFlagDirectlyViaTwoArgOverload() {
        ReactantDto dto = mapper.toDto(reactant, null);

        assertEquals(reactant.getId(), dto.uuid());
        assertEquals(reactant.getShortName(), dto.shortName());
        assertNull(dto.flag());
    }
}
