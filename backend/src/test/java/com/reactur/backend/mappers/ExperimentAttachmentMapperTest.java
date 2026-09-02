package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.experiments.ExperimentAttachmentDto;
import com.reactur.backend.entities.experiment.ExperimentAttachment;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves behavioral parity with the hand-written {@code FileServiceImpl.mapToDto}/
 * {@code mapToDetailsDto}/{@code getDownloadUrl} methods this mapper replaced - same field
 * mappings, same {@code "/api/file/{id}"} download URL shape.
 */
class ExperimentAttachmentMapperTest {

    private final ExperimentAttachmentMapper mapper = Mappers.getMapper(ExperimentAttachmentMapper.class);

    @Test
    void toDto_shouldMapFieldsAndComputeDownloadUrl() {
        UUID id = UUID.randomUUID();
        ExperimentAttachment entity = ExperimentAttachment.builder()
                .id(id)
                .fileName("measurement-data.csv")
                .fileType("text/csv")
                .fileSizeBytes(2048L)
                .checksumSha256("deadbeef")
                .archivedAt(Instant.now())
                .build();

        ExperimentAttachmentDto dto = mapper.toDto(entity);

        assertEquals(id, dto.uuid());
        assertEquals("measurement-data.csv", dto.fileName());
        assertEquals("text/csv", dto.fileType());
        assertEquals(2048L, dto.fileSizeBytes());
        assertEquals("/api/file/" + id, dto.downloadUrl());
    }

    @Test
    void toDetailsDto_shouldMapFieldsIncludingChecksumAndComputeDownloadUrl() {
        UUID id = UUID.randomUUID();
        ExperimentAttachment entity = ExperimentAttachment.builder()
                .id(id)
                .fileName("structure.xyz")
                .fileType("chemical/x-xyz")
                .fileSizeBytes(512L)
                .checksumSha256("cafebabe")
                .build();

        ExperimentAttachmentDetailsDto dto = mapper.toDetailsDto(entity);

        assertEquals(id, dto.uuid());
        assertEquals("structure.xyz", dto.fileName());
        assertEquals("chemical/x-xyz", dto.fileType());
        assertEquals(512L, dto.fileSizeBytes());
        assertEquals("cafebabe", dto.checkSum());
        assertEquals("/api/file/" + id, dto.downloadUrl());
    }
}
