package com.reactur.backend.services;

import com.reactur.backend.enums.Role;
import com.reactur.backend.exceptions.FileStorageException;
import com.reactur.backend.exceptions.ResourceConflictException;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.experiments.ExperimentAttachmentDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Manages files attached to experiments: storage in SeaweedFS, metadata in the database.
 */
public interface FileService {

    /**
     * Stores a file in SeaweedFS and adds the file metadata to the database.
     * <p>
     * Follows the "modify" rules of the parent experiment: the requesting user must be its
     * creator, its designated scientist, or a privileged role (ADMIN/SUPERVISOR). Uploading to an
     * archived experiment is strictly forbidden, regardless of who is asking.
     * </p>
     *
     * @param experimentId       the UUID of the experiment the file should be associated with
     * @param file               the multipart file to store
     * @param requestingUserId   the internal ID of the authenticated user performing the upload
     * @param requestingUserRole the role of the authenticated user
     * @return {@link ExperimentAttachmentDetailsDto} containing the saved metadata and download URL
     * @throws IllegalArgumentException if the provided file is null or empty
     * @throws ResourceNotFoundException if no experiment exists for the given UUID
     * @throws AccessDeniedException if the user lacks ownership or privileged rights over the parent experiment
     * @throws ResourceConflictException if the parent experiment is archived
     * @throws FileStorageException if calculating the checksum or uploading to SeaweedFS fails
     */
    ExperimentAttachmentDetailsDto storeFile(UUID experimentId, MultipartFile file, String requestingUserId, Role requestingUserRole);

    /**
     * Loads the binary file data from SeaweedFS as a Resource for downloading.
     * <p>
     * Follows the "view" rules of the parent experiment: visible to any approved user while the
     * experiment is active, and restricted to its creator, designated scientist, or a privileged
     * role once archived.
     * </p>
     *
     * @param fileId             the UUID of the file metadata record
     * @param requestingUserId   the internal ID of the authenticated user
     * @param requestingUserRole the role of the authenticated user
     * @return {@link Resource} representing the file stream
     * @throws ResourceNotFoundException if no file metadata exists for the given ID
     * @throws AccessDeniedException if the user lacks view rights over the parent experiment
     * @throws FileStorageException if retrieving the file stream from SeaweedFS fails
     */
    Resource loadFileAsResource(UUID fileId, String requestingUserId, Role requestingUserRole);

    /**
     * Retrieves the details of a specific file attachment.
     * <p>
     * Follows the same "view" rules as {@link #loadFileAsResource}.
     * </p>
     *
     * @param fileId             the unique UUID identifier of the file
     * @param requestingUserId   the internal ID of the authenticated user
     * @param requestingUserRole the role of the authenticated user
     * @return an {@link ExperimentAttachmentDetailsDto} containing complete file attributes, including checksum and size
     * @throws ResourceNotFoundException if no file metadata exists for the given ID
     * @throws AccessDeniedException if the user lacks view rights over the parent experiment
     * */
    ExperimentAttachmentDetailsDto getAttachmentDetail(UUID fileId, String requestingUserId, Role requestingUserRole);

    /**
     * Retrieves all file attachments associated with a specific experiment.
     * <p>
     * Intentionally not secured: this is an internal helper used by
     * {@code ExperimentService} after the parent experiment's own view authorization has
     * already been checked, not a directly user-facing lookup.
     * </p>
     *
     * @param experimentId the unique UUID identifier of the experiment
     * @return a List of {@link ExperimentAttachmentDto} containing the overview and download information of the attachments
     * @throws ResourceNotFoundException if no experiment exists for the given ID
     * */
    List<ExperimentAttachmentDto> getAttachmentsOfExperiment(UUID experimentId);

    /**
     * Archives (soft-deletes) a file.
     * <p>
     * Follows the "modify" rules of the parent experiment: the requesting user must be its
     * creator, its designated scientist, or a privileged role (ADMIN/SUPERVISOR). Deleting a file
     * from an archived experiment is strictly forbidden, regardless of who is asking.
     * </p>
     *
     * @param fileId             the UUID of the file to archive
     * @param requestingUserId   the internal ID of the authenticated user
     * @param requestingUserRole the role of the authenticated user
     * @throws ResourceNotFoundException if no file metadata exists for the given ID
     * @throws AccessDeniedException if the user lacks ownership or privileged rights over the parent experiment
     * @throws ResourceConflictException if the parent experiment is archived
     */
    void archiveFile(UUID fileId, String requestingUserId, Role requestingUserRole);
}
