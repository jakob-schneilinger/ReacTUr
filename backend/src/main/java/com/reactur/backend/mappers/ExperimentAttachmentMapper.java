package com.reactur.backend.mappers;

import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.experiments.ExperimentAttachmentDto;
import com.reactur.backend.entities.experiment.ExperimentAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

/**
 * Maps {@link ExperimentAttachment} to its two response DTOs.
 * <p>
 * Extracted from {@code FileServiceImpl}'s previously hand-written {@code mapToDto}/
 * {@code mapToDetailsDto} private methods, for consistency with how every other entity in this
 * codebase is mapped (via a dedicated MapStruct interface, not manual field-by-field construction).
 * See the resolved "ExperimentAttachmentMapper?" TODO in {@code ExperimentMapper.java}.
 */
@Mapper(componentModel = "spring")
public interface ExperimentAttachmentMapper {

    @Mapping(target = "uuid", source = "id")
    @Mapping(target = "downloadUrl", source = "id", qualifiedByName = "computeDownloadUrl")
    ExperimentAttachmentDto toDto(ExperimentAttachment entity);

    @Mapping(target = "uuid", source = "id")
    @Mapping(target = "checkSum", source = "checksumSha256")
    @Mapping(target = "downloadUrl", source = "id", qualifiedByName = "computeDownloadUrl")
    ExperimentAttachmentDetailsDto toDetailsDto(ExperimentAttachment entity);

    /**
     * Same computation as the original {@code FileServiceImpl.getDownloadUrl} - byte-for-byte
     * identical URL shape, just relocated here.
     */
    @Named("computeDownloadUrl")
    default String computeDownloadUrl(UUID id) {
        return String.format("/api/file/%s", id);
    }
}
