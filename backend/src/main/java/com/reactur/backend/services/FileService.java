package com.reactur.backend.services;

import org.springframework.core.io.Resource;
import com.reactur.backend.dtos.ExperimentDtos.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.ExperimentDtos.ExperimentAttachmentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileService {

    /**
     * Stores a file in SeaweedFS and adds the file metadata to the database.
     *
     * @param experimentId the UUID of the experiment the file should be associated with
     * @param file the multipart file to store
     * @return {@link ExperimentAttachmentDetailsDto} containing the saved metadata and download URL
     */
    ExperimentAttachmentDetailsDto storeFile(UUID experimentId, MultipartFile file);

    /**
     * Loads the binary file data from SeaweedFS as a Resource for downloading.
     *
     * @param fileId the UUID of the file metadata record
     * @return {@link Resource} representing the file stream
     */
    Resource loadFileAsResource(UUID fileId);

    /**
     * Retrieves the details of a specific file attachment.
     *
     * @param fileId the unique UUID identifier of the file
     * @return an {@link ExperimentAttachmentDetailsDto} containing complete file attributes, including checksum and size
     */
    ExperimentAttachmentDetailsDto getAttachmentDetail(UUID fileId);

    /**
     * Retrieves all file attachments associated with a specific experiment.
     *
     * @param experimentId the unique UUID identifier of the experiment
     * @return a List of {@link ExperimentAttachmentDto} containing the overview and download information of the attachments
     */
    List<ExperimentAttachmentDto> getAttachmentsOfExperiment(UUID experimentId);

    /**
     * Archives a file.
     *
     * @param fileId the UUID of the file to archive
     */
    void archiveFile(UUID fileId);
}
