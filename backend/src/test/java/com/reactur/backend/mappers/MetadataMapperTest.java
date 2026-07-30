package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.GetMetadataDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.enums.Role;
import com.reactur.backend.mappers.options.InstrumentMapper;
import com.reactur.backend.mappers.options.ModalityMapper;
import com.reactur.backend.mappers.options.SolventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataMapperTest {

    private final MetadataMapper mapper = Mappers.getMapper(MetadataMapper.class);

    private Metadata metadata;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);
        SolventMapper solventMapper = Mappers.getMapper(SolventMapper.class);
        InstrumentMapper instrumentMapper = Mappers.getMapper(InstrumentMapper.class);
        ModalityMapper modalityMapper = Mappers.getMapper(ModalityMapper.class);

        // MetadataMapperImpl relies on Spring field injection for its `uses` mappers, so for a
        // plain unit test (no Spring context) we have to wire them in manually.
        ReflectionTestUtils.setField(mapper, "userMapper", userMapper);
        ReflectionTestUtils.setField(mapper, "solventMapper", solventMapper);
        ReflectionTestUtils.setField(mapper, "instrumentMapper", instrumentMapper);
        ReflectionTestUtils.setField(mapper, "modalityMapper", modalityMapper);

        User scientist = User.builder().id("scientist-1").firstName("Ada").lastName("Lovelace").role(Role.USER).build();
        Solvent solvent = Solvent.builder().id(UUID.randomUUID()).name("Water").build();
        Instrument instrument = Instrument.builder().id(UUID.randomUUID()).name("UV-Vis").build();
        Modality modality = Modality.builder().id(UUID.randomUUID()).name("Absorbance").build();

        metadata = Metadata.builder()
                .id(UUID.randomUUID())
                .date(LocalDate.of(2024, 1, 1))
                .scientist(scientist)
                .temperature(25.0)
                .solvent(solvent)
                .concentrationA(1.0)
                .concentrationB(2.0)
                .instrument(instrument)
                .modality(modality)
                .wavelength(450.0)
                .comment("test")
                .build();
    }

    @Test
    void metadataToGetMetadataDto_shouldMapIdToUuid_andNestedOptionEntities() {
        GetMetadataDto dto = mapper.metadataToGetMetadataDto(metadata);

        assertEquals(metadata.getId(), dto.uuid());
        assertEquals(metadata.getDate(), dto.date());
        assertEquals(metadata.getScientist().getId(), dto.scientist().id());
        assertEquals(metadata.getTemperature(), dto.temperature());
        assertEquals(metadata.getSolvent().getId(), dto.solvent().uuid());
        assertEquals(metadata.getSolvent().getName(), dto.solvent().name());
        assertEquals(metadata.getConcentrationA(), dto.concentrationA());
        assertEquals(metadata.getConcentrationB(), dto.concentrationB());
        assertEquals(metadata.getInstrument().getId(), dto.instrument().uuid());
        assertEquals(metadata.getModality().getId(), dto.modality().uuid());
        assertEquals(metadata.getWavelength(), dto.wavelength());
        assertEquals(metadata.getComment(), dto.comment());
    }
}
