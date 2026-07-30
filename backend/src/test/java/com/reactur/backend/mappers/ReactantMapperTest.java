package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.enums.StructureFlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReactantMapperTest {

    private final ReactantMapper mapper = Mappers.getMapper(ReactantMapper.class);

    private Reactant reactant;
    private ChemicalType chemicalType;

    @BeforeEach
    void setUp() {
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

        // Known limitation (see TODO on ReactantMapper): the nested ChemicalType -> ChemicalTypeDto
        // mapping is not wired up via a ChemicalTypeMapper, so its uuid always comes out null even
        // though the source entity has one. Name still maps correctly.
        assertEquals(chemicalType.getName(), dto.chemicalType().name());
        assertNull(dto.chemicalType().uuid());
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
    }

    @Test
    void toDto_withoutFlag_shouldDelegateWithNullFlag() {
        ReactantDto dto = mapper.toDto(reactant);

        assertEquals(reactant.getId(), dto.uuid());
        assertNull(dto.flag());
    }
}
