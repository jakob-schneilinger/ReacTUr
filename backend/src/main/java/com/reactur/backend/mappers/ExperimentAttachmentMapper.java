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
     * Builds the URL clients use to download this file, by its attachment ID.
     *
     * @param id the attachment's ID
     * @return the download URL
     */
    @Named("computeDownloadUrl")
    default String computeDownloadUrl(UUID id) {
        return String.format("/api/file/%s", id);
    }
}
