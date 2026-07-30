package com.reactur.backend.mappers;

import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.enums.StructureFlag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StructureMapperTest {

    private final StructureMapper mapper = Mappers.getMapper(StructureMapper.class);

    @Test
    void toDto_shouldMapReactantToReactantIdAndFileNameToName() {
        Reactant reactant = Reactant.builder().id(UUID.randomUUID()).build();
        ReactantStructure entity = ReactantStructure.builder()
                .id(UUID.randomUUID())
                .reactant(reactant)
                .fileName("molecule.xyz")
                .structure("0 1\nC 0.0 0.0 0.0")
                .externalJobId("job-1")
                .flag(StructureFlag.ACTIVE)
                .build();

        StructureDto dto = mapper.toDto(entity);

        assertEquals(entity.getId(), dto.id());
        assertEquals(reactant.getId(), dto.reactant());
        assertEquals(entity.getFileName(), dto.name());
        assertEquals(entity.getStructure(), dto.structure());
        assertEquals(entity.getFlag(), dto.flag());
    }

    @Test
    void toDto_shouldMapNullReactantToNullReactantId() {
        ReactantStructure entity = ReactantStructure.builder()
                .id(UUID.randomUUID())
                .reactant(null)
                .fileName("molecule.xyz")
                .build();

        StructureDto dto = mapper.toDto(entity);

        assertNull(dto.reactant());
    }
}
