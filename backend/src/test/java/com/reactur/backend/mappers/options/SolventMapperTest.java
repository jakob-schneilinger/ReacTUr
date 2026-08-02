package com.reactur.backend.mappers.options;

import com.reactur.backend.dtos.options.SolventDto;
import com.reactur.backend.entities.options.Solvent;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Representative test for the {@code BaseMapper}-based options mappers (InstrumentMapper,
 * SolventMapper, ModalityMapper, ChemicalTypeMapper). All four share an identical generic
 * contract via BaseMapper, so this single suite is enough to confirm that contract works;
 * it's not repeated for each of the other three.
 */
class SolventMapperTest {

    private final SolventMapper mapper = Mappers.getMapper(SolventMapper.class);

    @Test
    void toDto_shouldMapIdToUuid() {
        Instant archivedAt = Instant.now();
        Solvent entity = Solvent.builder().id(UUID.randomUUID()).name("Water").archivedAt(archivedAt).build();

        SolventDto dto = mapper.toDto(entity);

        assertEquals(entity.getId(), dto.uuid());
        assertEquals(entity.getName(), dto.name());
        assertEquals(archivedAt, dto.archivedAt());
    }

    @Test
    void toEntity_shouldIgnoreClientSuppliedUuidAndArchivedAt_soIdIsServerGeneratedAndOptionStartsUnarchived() {
        UUID uuid = UUID.randomUUID();
        Instant archivedAt = Instant.now();
        SolventDto dto = new SolventDto(uuid, "Ethanol", archivedAt);

        Solvent entity = mapper.toEntity(dto);

        assertNull(entity.getId());
        assertEquals(dto.name(), entity.getName());
        assertNull(entity.getArchivedAt());
    }

    @Test
    void toDtoList_shouldMapEachEntity() {
        Solvent a = Solvent.builder().id(UUID.randomUUID()).name("Water").build();
        Solvent b = Solvent.builder().id(UUID.randomUUID()).name("Ethanol").build();

        List<SolventDto> dtos = mapper.toDtoList(List.of(a, b));

        assertEquals(2, dtos.size());
        assertEquals(a.getId(), dtos.get(0).uuid());
        assertEquals(b.getId(), dtos.get(1).uuid());
    }
}
