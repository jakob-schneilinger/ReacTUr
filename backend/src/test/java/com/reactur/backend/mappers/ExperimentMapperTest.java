package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.ExperimentAttachmentDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentAttachment;
import com.reactur.backend.entities.experiment.ExperimentResult;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentMapperTest {

    private final ExperimentMapper mapper = Mappers.getMapper(ExperimentMapper.class);

    private Experiment experiment;

    @BeforeEach
    void setUp() {
        // ExperimentMapperImpl (and the mappers it delegates to) rely on Spring field injection
        // for their `uses` mappers, so for a plain unit test (no Spring context) we have to wire
        // them in manually, mirroring the pattern used in ExperimentServiceTest.
        ReactantMapper reactantMapper = Mappers.getMapper(ReactantMapper.class);
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);
        MeasurementDataMapper measurementDataMapper = Mappers.getMapper(MeasurementDataMapper.class);
        MetadataMapper metadataMapper = Mappers.getMapper(MetadataMapper.class);
        SolventMapper solventMapper = Mappers.getMapper(SolventMapper.class);
        InstrumentMapper instrumentMapper = Mappers.getMapper(InstrumentMapper.class);
        ModalityMapper modalityMapper = Mappers.getMapper(ModalityMapper.class);

        ReflectionTestUtils.setField(metadataMapper, "userMapper", userMapper);
        ReflectionTestUtils.setField(metadataMapper, "solventMapper", solventMapper);
        ReflectionTestUtils.setField(metadataMapper, "instrumentMapper", instrumentMapper);
        ReflectionTestUtils.setField(metadataMapper, "modalityMapper", modalityMapper);

        ReflectionTestUtils.setField(mapper, "metadataMapper", metadataMapper);
        ReflectionTestUtils.setField(mapper, "measurementDataMapper", measurementDataMapper);
        ReflectionTestUtils.setField(mapper, "reactantMapper", reactantMapper);
        ReflectionTestUtils.setField(mapper, "userMapper", userMapper);

        User createdBy = User.builder().id("creator-1").firstName("Grace").lastName("Hopper").role(Role.USER).build();
        Reactant reactantA = Reactant.builder().id(UUID.randomUUID()).shortName("A").smiles("C").build();
        Reactant reactantB = Reactant.builder().id(UUID.randomUUID()).shortName("B").smiles("O").build();
        Metadata metadata = Metadata.builder()
                .id(UUID.randomUUID())
                .date(LocalDate.of(2024, 6, 1))
                .scientist(createdBy)
                .solvent(Solvent.builder().id(UUID.randomUUID()).name("Water").build())
                .instrument(Instrument.builder().id(UUID.randomUUID()).name("UV-Vis").build())
                .modality(Modality.builder().id(UUID.randomUUID()).name("Absorbance").build())
                .build();
        ExperimentResult result = ExperimentResult.builder()
                .rateConstant(1.5)
                .errorValue(0.2)
                .isErrorPercentage(false)
                .build();

        experiment = Experiment.builder()
                .id(UUID.randomUUID())
                .reactantA(reactantA)
                .reactantB(reactantB)
                .dois(List.of("10.1000/rxn123"))
                .metadata(metadata)
                .result(result)
                .files(List.of())
                .createdBy(createdBy)
                .build();
    }

    @Test
    void experimentToGetExperimentDto_withExplicitAttachments_shouldMapIdAndUseProvidedAttachments() {
        List<ExperimentAttachmentDto> attachments = List.of(
                new ExperimentAttachmentDto(UUID.randomUUID(), "file.csv", "text/csv", 123L, "http://example.com/file.csv")
        );

        GetExperimentDto dto = mapper.experimentToGetExperimentDto(experiment, attachments);

        assertEquals(experiment.getId(), dto.uuid());
        assertEquals(experiment.getDois(), dto.dois());
        assertEquals(attachments, dto.attachments());
        assertEquals(experiment.getReactantA().getId(), dto.reactantA().uuid());
        assertEquals(experiment.getReactantB().getId(), dto.reactantB().uuid());
        assertEquals(experiment.getResult().getRateConstant(), dto.measurementData().rateConstant());
        assertEquals(experiment.getMetadata().getId(), dto.metadata().uuid());
        assertEquals(experiment.getCreatedBy().getId(), dto.createdBy().id());
    }

    @Test
    void experimentToGetExperimentDto_singleArgOverload_shouldMapIdAndDeriveAttachmentsFromFiles() {
        GetExperimentDto dto = mapper.experimentToGetExperimentDto(experiment);

        assertEquals(experiment.getId(), dto.uuid());
        assertTrue(dto.attachments().isEmpty());
        assertEquals(experiment.getResult().getRateConstant(), dto.measurementData().rateConstant());
        assertEquals(experiment.getReactantA().getId(), dto.reactantA().uuid());
    }

    @Test
    void experimentToGetExperimentDto_withExplicitAttachments_shouldMapNullResultAndMetadata_toNull() {
        experiment.setResult(null);
        experiment.setMetadata(null);

        GetExperimentDto dto = mapper.experimentToGetExperimentDto(experiment, List.of());

        assertNull(dto.measurementData());
        assertNull(dto.metadata());
    }

    @Test
    void experimentToGetExperimentDto_singleArgOverload_shouldDeriveNonEmptyAttachmentsFromFiles() {
        ExperimentAttachment attachment = ExperimentAttachment.builder()
                .id(UUID.randomUUID())
                .fileName("data.csv")
                .fileType("text/csv")
                .fileSizeBytes(456L)
                .build();
        experiment.setFiles(List.of(attachment));

        GetExperimentDto dto = mapper.experimentToGetExperimentDto(experiment);

        assertEquals(1, dto.attachments().size());
        assertEquals(attachment.getFileName(), dto.attachments().get(0).fileName());
        assertEquals(attachment.getFileType(), dto.attachments().get(0).fileType());
        assertEquals(attachment.getFileSizeBytes(), dto.attachments().get(0).fileSizeBytes());
    }
}
