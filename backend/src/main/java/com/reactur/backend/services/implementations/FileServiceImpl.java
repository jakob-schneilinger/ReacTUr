package com.reactur.backend.services.implementations;

import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.FileStorageException;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.services.FileService;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.experiments.ExperimentAttachmentDto;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentAttachment;
import com.reactur.backend.mappers.ExperimentAttachmentMapper;
import com.reactur.backend.repositories.ExperimentAttachmentRepository;
import com.reactur.backend.repositories.ExperimentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final ExperimentAttachmentRepository attachmentRepository;
    private final ExperimentRepository experimentRepository;
    private final ExperimentAuthorization experimentAuthorization;
    private final ExperimentAttachmentMapper attachmentMapper;

    private final S3Client s3Client;

    @Value("${seaweedfs.s3.bucket:experiment-files}")
    private String bucketName;

    /**
     * Provisional per-attachment size cap in bytes (default 10MB), matching the limit the frontend's
     * FileUploadComponent already enforces for experiment attachments (see
     * experiment-details-page.component.html's {@code [maxFileSizeMB]="10"}). This is NOT a finalized
     * product decision - see Software_todo.md item 14 - just a stop-gap so the backend enforces
     * something instead of relying solely on the frontend and Spring's blanket
     * {@code spring.servlet.multipart.max-file-size}. Deployers can override via
     * {@code app.upload.experiment-attachment.max-size-bytes} without a code change.
     * TODO: replace with a properly decided, tested limit (and matching file-type restrictions) once
     * product/Dennis decide the real policy - see Software_todo.md item 14.
     */
    @Value("${app.upload.experiment-attachment.max-size-bytes:10485760}")
    private long maxAttachmentSizeBytes = 10_485_760L;

    @Override
    @Transactional
    public ExperimentAttachmentDetailsDto storeFile(UUID experimentId, MultipartFile file, String requestingUserId, Role requestingUserRole) {
        if (file == null || file.isEmpty()) {
            log.warn("Upload failed: Attempted to upload an empty file for experiment ID: {}", experimentId);
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        if (file.getSize() > maxAttachmentSizeBytes) {
            log.warn("Upload rejected: file size {} bytes exceeds the configured limit of {} bytes for experiment ID: {}",
                    file.getSize(), maxAttachmentSizeBytes, experimentId);
            throw new IllegalArgumentException("Uploaded file exceeds the maximum allowed size of " + maxAttachmentSizeBytes + " bytes.");
        }

        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment with ID " + experimentId + " not found"));

        experimentAuthorization.assertCanModify(experiment, requestingUserId, requestingUserRole);

        if (experiment.getArchivedAt() != null) {
            throw new ResourceConflictException("Cannot upload a file to an archived experiment. Please restore it first.");
        }

        String checksum = calculateSha256(file);
        String storageReferenceId = UUID.randomUUID() + "-" + file.getOriginalFilename();

        log.info("Sending file '{}' to SeaweedFS (S3) Bucket: {}", file.getOriginalFilename(), bucketName);
        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageReferenceId)
                .contentType(file.getContentType())
                .build();

        try (InputStream is = file.getInputStream()) {
            s3Client.putObject(putOb, RequestBody.fromInputStream(is, file.getSize()));
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload file to S3", e);
        }

        // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): the archived-state
        // check above and this method's eventual attachment save straddle a real S3 network call, which can take
        // seconds - too long to hold a DB row lock across (that would block every other request touching this
        // experiment for the duration of the upload). Instead, re-check the experiment's CURRENT archived state with
        // a fresh, unlocked read immediately after the S3 upload completes and before persisting the attachment. If
        // a concurrent archiveExperiment() landed in between, delete the just-uploaded S3 object (same compensating
        // pattern as the DB-save-failure case below) and reject, instead of silently attaching a file to what is now
        // an archived experiment.
        boolean archivedDuringUpload = experimentRepository.findById(experimentId)
                .map(fresh -> fresh.getArchivedAt() != null)
                .orElse(true);
        if (archivedDuringUpload) {
            log.warn("Experiment {} was archived (or deleted) while file '{}' was being uploaded to S3; deleting the orphaned object '{}'.",
                    experimentId, file.getOriginalFilename(), storageReferenceId);
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storageReferenceId)
                        .build());
            } catch (Exception deleteException) {
                log.error("Failed to delete orphaned S3 object '{}' from bucket '{}' after a concurrent archive. Manual cleanup required.",
                        storageReferenceId, bucketName, deleteException);
            }
            throw new ResourceConflictException("Cannot upload a file to an archived experiment. Please restore it first.");
        }

        ExperimentAttachment attachment = ExperimentAttachment.builder()
                .experiment(experiment)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .storageReferenceId(storageReferenceId)
                .checksumSha256(checksum)
                .fileSizeBytes(file.getSize())
                .build();

        ExperimentAttachment saved;
        try {
            saved = attachmentRepository.save(attachment);
        } catch (Exception e) {
            log.error("Failed to save attachment metadata for file '{}' after it was already uploaded to S3. Attempting to delete orphaned object '{}' from bucket '{}'.",
                    file.getOriginalFilename(), storageReferenceId, bucketName, e);
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storageReferenceId)
                        .build());
            } catch (Exception deleteException) {
                log.error("Failed to delete orphaned S3 object '{}' from bucket '{}' after database save failure. Manual cleanup required.",
                        storageReferenceId, bucketName, deleteException);
            }
            if (e instanceof FileStorageException fileStorageException) {
                throw fileStorageException;
            }
            throw new FileStorageException("Failed to save file metadata to database", e);
        }
        log.info("Successfully stored file metadata in database with attachment ID: {}", saved.getId());

        return attachmentMapper.toDetailsDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadFileAsResource(UUID fileId, String requestingUserId, Role requestingUserRole) {
        log.trace("Starting download for file ID: {}", fileId);
        ExperimentAttachment attachment = attachmentRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File ID " + fileId + " not found"));

        experimentAuthorization.assertCanView(attachment.getExperiment(), requestingUserId, requestingUserRole);

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(attachment.getStorageReferenceId())
                .build();

        try {
            return new InputStreamResource(s3Client.getObject(getRequest));
        } catch (Exception e) {
            throw new FileStorageException("Failed to retrieve file from S3", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentAttachmentDetailsDto getAttachmentDetail(UUID fileId, String requestingUserId, Role requestingUserRole) {
        ExperimentAttachment attachment = attachmentRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File ID " + fileId + " not found"));

        experimentAuthorization.assertCanView(attachment.getExperiment(), requestingUserId, requestingUserRole);

        return attachmentMapper.toDetailsDto(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperimentAttachmentDto> getAttachmentsOfExperiment(UUID experimentId) {
        log.trace("Fetching all active attachments for experiment ID: {}", experimentId);

        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment with ID " + experimentId + " not found");
        }

        List<ExperimentAttachment> attachments = attachmentRepository.findByExperimentIdAndArchivedAtIsNull(experimentId);
        log.trace("Found {} active attachments for experiment ID: {}", attachments.size(), experimentId);

        return attachments.stream()
                .map(attachmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void archiveFile(UUID fileId, String requestingUserId, Role requestingUserRole) {
        log.info("Attempting to archive file ID: {}", fileId);

        ExperimentAttachment attachment = attachmentRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File ID " + fileId + " not found"));

        experimentAuthorization.assertCanModify(attachment.getExperiment(), requestingUserId, requestingUserRole);

        if (attachment.getExperiment().getArchivedAt() != null) {
            throw new ResourceConflictException("Cannot delete a file from an archived experiment. Please restore it first.");
        }

        if (attachment.getArchivedAt() != null) {
            log.info("File ID {} is already archived.", fileId);
            return;
        }

        // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): the check above
        // reads the experiment's archived state from the already-loaded attachment.getExperiment() object, which
        // would not reflect a concurrent archiveExperiment() commit that happened after this method's own read. This
        // second check issues a genuinely fresh, unlocked query for the parent experiment immediately before this
        // method's save, closing that window without needing a pessimistic lock (there is no external I/O between
        // here and the save to justify one).
        UUID parentExperimentId = attachment.getExperiment().getId();
        boolean archivedConcurrently = experimentRepository.findById(parentExperimentId)
                .map(fresh -> fresh.getArchivedAt() != null)
                .orElse(true);
        if (archivedConcurrently) {
            throw new ResourceConflictException("Cannot delete a file from an archived experiment. Please restore it first.");
        }

        attachment.setArchivedAt(Instant.now());
        attachmentRepository.save(attachment);

        log.info("Successfully soft-archived file ID: {}", fileId);
    }


    private String calculateSha256(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = is.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            byte[] bytes = digest.digest();
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new FileStorageException("Failed to calculate file checksum", e);
        }
    }
}
