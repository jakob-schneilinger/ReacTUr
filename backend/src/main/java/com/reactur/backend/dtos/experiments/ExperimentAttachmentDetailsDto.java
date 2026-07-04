package com.reactur.backend.dtos.experiments;

import java.util.UUID;

/**
 * DTO representing a file attached to an experiment in detail.
 *
 * @param uuid          The uuid of the file.
 * @param fileName      The original name of the uploaded file.
 * @param fileType      The MIME type or file extension.
 * @param fileSizeBytes The total size of the file in bytes.
 * @param checkSum      The sha-256 checksum.
 * @param downloadUrl   The URL used to download the file.
 */
public record ExperimentAttachmentDetailsDto(
        UUID uuid,
        String fileName,
        String fileType,
        Long fileSizeBytes,
        String checkSum,
        String downloadUrl
) {
}
