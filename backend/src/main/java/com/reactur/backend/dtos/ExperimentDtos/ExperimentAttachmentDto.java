package com.reactur.backend.dtos.ExperimentDtos;

import java.util.UUID;

/**
 * DTO representing a file attached to an experiment.
 *
 * @param uuid          The uuid of the file attachment.
 * @param fileName      The original name of the uploaded file.
 * @param fileType      The MIME type or file extension.
 * @param fileSizeBytes The total size of the file in bytes.
 * @param downloadUrl   The URL used to download the file.
 */
public record ExperimentAttachmentDto(
        UUID uuid,
        String fileName,
        String fileType,
        Long fileSizeBytes,
        String downloadUrl
) {
}
