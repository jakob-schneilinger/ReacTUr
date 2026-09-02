package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.experiments.ExperimentAttachmentDto;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentAttachment;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.FileStorageException;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.ExperimentAttachmentMapper;
import com.reactur.backend.repositories.ExperimentAttachmentRepository;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import com.reactur.backend.services.implementations.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTests {

    @Mock
    private ExperimentAttachmentRepository attachmentRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private ExperimentAuthorization experimentAuthorization;

    @Spy
    private ExperimentAttachmentMapper attachmentMapper = Mappers.getMapper(ExperimentAttachmentMapper.class);

    @InjectMocks
    private FileServiceImpl fileService;

    private final String BUCKET_NAME = "test-bucket";

    // Note: experimentAuthorization is a plain Mockito @Mock, so assertCanView/assertCanModify don't throw
    // anything by default. These tests focus on storage behavior, not authorization,
    // which are covered separately by ExperimentAuthorization's own tests.
    private static final String REQUESTING_USER_ID = "requesting-user-id";
    private static final Role REQUESTING_USER_ROLE = Role.ADMIN;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "bucketName", BUCKET_NAME);
    }

    @Test
    void storeFile_ShouldStoreSuccessfully() {

        UUID experimentId = UUID.randomUUID();
        Experiment experiment = new Experiment(); // Set required fields if needed

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "Dummy Content".getBytes());

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        when(attachmentRepository.save(any(ExperimentAttachment.class))).thenAnswer(invocation -> {
            ExperimentAttachment savedAttachment = invocation.getArgument(0);
            savedAttachment.setId(UUID.randomUUID());
            return savedAttachment;
        });

        ExperimentAttachmentDetailsDto result = fileService.storeFile(experimentId, file, REQUESTING_USER_ID, REQUESTING_USER_ROLE);

        assertNotNull(result);
        assertEquals("test.pdf", result.fileName());
        verify(attachmentRepository, times(1)).save(any(ExperimentAttachment.class));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void storeFile_ShouldThrowNotFound_WhenExperimentDoesNotExist() {
        UUID experimentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Test".getBytes());

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> fileService.storeFile(experimentId, file, REQUESTING_USER_ID, REQUESTING_USER_ROLE));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void storeFile_ShouldDeleteOrphanedS3ObjectAndThrowConflict_WhenExperimentArchivedDuringUpload() {
        // Covers the race condition fix documented in Software_todo.md's Race condition inventory item: the
        // archived-state check and the attachment save straddle a real S3 call, so storeFile re-checks the
        // experiment's current state via a second, fresh findById immediately after the S3 upload completes.
        UUID experimentId = UUID.randomUUID();
        Experiment notYetArchived = new Experiment();
        Experiment archivedDuringUpload = new Experiment();
        archivedDuringUpload.setArchivedAt(Instant.now());

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "Dummy Content".getBytes());

        when(experimentRepository.findById(experimentId))
                .thenReturn(Optional.of(notYetArchived))
                .thenReturn(Optional.of(archivedDuringUpload));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        assertThrows(ResourceConflictException.class,
                () -> fileService.storeFile(experimentId, file, REQUESTING_USER_ID, REQUESTING_USER_ROLE));

        verify(experimentRepository, times(2)).findById(experimentId);
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void storeFile_ShouldThrowResourceConflict_WhenExperimentIsArchived() {
        UUID experimentId = UUID.randomUUID();
        Experiment experiment = new Experiment();
        experiment.setArchivedAt(Instant.now());

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "Dummy Content".getBytes());

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));

        assertThrows(ResourceConflictException.class,
                () -> fileService.storeFile(experimentId, file, REQUESTING_USER_ID, REQUESTING_USER_ROLE));

        verify(attachmentRepository, never()).save(any());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void storeFile_ShouldThrowIllegalArgumentException_WhenFileIsEmpty() {
        UUID experimentId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileService.storeFile(experimentId, emptyFile, REQUESTING_USER_ID, REQUESTING_USER_ROLE)
        );

        assertEquals("Cannot upload an empty file.", exception.getMessage());

        verify(experimentRepository, never()).findById(any());
        verify(attachmentRepository, never()).save(any());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void storeFile_ShouldThrowFileStorageException_WhenS3PutObjectFails() {
        UUID experimentId = UUID.randomUUID();
        Experiment experiment = new Experiment();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "Dummy Content".getBytes());

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 unavailable"));

        assertThrows(FileStorageException.class,
                () -> fileService.storeFile(experimentId, file, REQUESTING_USER_ID, REQUESTING_USER_ROLE));

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void storeFile_ShouldDeleteOrphanedS3Object_WhenDatabaseSaveFails() {
        UUID experimentId = UUID.randomUUID();
        Experiment experiment = new Experiment();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "Dummy Content".getBytes());

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        when(attachmentRepository.save(any(ExperimentAttachment.class)))
                .thenThrow(new RuntimeException("DB unavailable"));

        assertThrows(FileStorageException.class,
                () -> fileService.storeFile(experimentId, file, REQUESTING_USER_ID, REQUESTING_USER_ROLE));

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void storeFile_ShouldStillThrowOriginalFailure_WhenCompensatingS3DeleteAlsoFails() {
        UUID experimentId = UUID.randomUUID();
        Experiment experiment = new Experiment();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "Dummy Content".getBytes());

        when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        when(attachmentRepository.save(any(ExperimentAttachment.class)))
                .thenThrow(new RuntimeException("DB unavailable"));

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("S3 delete also failed"));

        assertThrows(FileStorageException.class,
                () -> fileService.storeFile(experimentId, file, REQUESTING_USER_ID, REQUESTING_USER_ROLE));
    }

    @Test
    void loadFileAsResource_ShouldReturnResource() {
        UUID fileId = UUID.randomUUID();
        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setStorageReferenceId("s3-key-123");
        attachment.setExperiment(new Experiment());

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        ResponseInputStream<GetObjectResponse> mockStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);

        Resource result = fileService.loadFileAsResource(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE);

        assertNotNull(result);
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void loadFileAsResource_ShouldThrowNotFound_WhenFileDoesNotExist() {
        UUID fileId = UUID.randomUUID();
        when(attachmentRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> fileService.loadFileAsResource(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE));

        verify(s3Client, never()).getObject(any(GetObjectRequest.class));
    }

    @Test
    void loadFileAsResource_ShouldThrowFileStorageException_WhenS3GetObjectFails() {
        UUID fileId = UUID.randomUUID();
        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setStorageReferenceId("s3-key-123");
        attachment.setExperiment(new Experiment());

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("S3 unavailable"));

        assertThrows(FileStorageException.class,
                () -> fileService.loadFileAsResource(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE));
    }

    @Test
    void getAttachmentDetail_ShouldReturnDto() {
        UUID fileId = UUID.randomUUID();
        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setId(fileId);
        attachment.setFileName("data.csv");
        attachment.setFileSizeBytes(1024L);
        attachment.setExperiment(new Experiment());

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        ExperimentAttachmentDetailsDto result = fileService.getAttachmentDetail(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE);

        assertEquals("data.csv", result.fileName());
        assertEquals(1024L, result.fileSizeBytes());
    }

    @Test
    void getAttachmentDetail_ShouldThrowNotFound() {
        UUID fileId = UUID.randomUUID();
        when(attachmentRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fileService.getAttachmentDetail(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE));
    }

    @Test
    void getAttachmentsOfExperiment_ShouldReturnList() {
        UUID experimentId = UUID.randomUUID();
        when(experimentRepository.existsById(experimentId)).thenReturn(true);

        ExperimentAttachment att1 = new ExperimentAttachment();
        att1.setId(UUID.randomUUID());
        att1.setFileName("file1.txt");

        when(attachmentRepository.findByExperimentIdAndArchivedAtIsNull(experimentId))
                .thenReturn(List.of(att1));

        List<ExperimentAttachmentDto> result = fileService.getAttachmentsOfExperiment(experimentId);

        assertEquals(1, result.size());
        assertEquals("file1.txt", result.getFirst().fileName());
    }

    @Test
    void getAttachmentsOfExperiment_ShouldThrowNotFound_WhenExperimentDoesNotExist() {
        UUID experimentId = UUID.randomUUID();
        when(experimentRepository.existsById(experimentId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> fileService.getAttachmentsOfExperiment(experimentId));
    }

    @Test
    void archiveFile_ShouldThrowNotFound_WhenFileDoesNotExist() {
        UUID fileId = UUID.randomUUID();
        when(attachmentRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fileService.archiveFile(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void archiveFile_ShouldArchiveSuccessfully_WhenFileIsActive() {
        UUID fileId = UUID.randomUUID();
        Experiment experiment = new Experiment();
        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setArchivedAt(null);
        attachment.setExperiment(experiment);

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));
        // FileServiceImpl.archiveFile re-checks the parent experiment's current archived state with a fresh
        // repository read immediately before saving (see the Race condition inventory item in Software_todo.md).
        when(experimentRepository.findById(any())).thenReturn(Optional.of(experiment));

        fileService.archiveFile(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE);

        verify(attachmentRepository, times(1)).save(attachment);
    }

    @Test
    void archiveFile_ShouldThrowResourceConflict_WhenExperimentArchivedConcurrently() {
        // Covers the fresh-recheck fix (see Race condition inventory item in Software_todo.md): the FIRST check
        // (against attachment.getExperiment(), a snapshot loaded at the top of the method) still sees the
        // experiment as active, but the second, fresh experimentRepository.findById() read - issued right before
        // the save - sees that it was archived by a concurrent request in the meantime.
        UUID fileId = UUID.randomUUID();
        Experiment staleSnapshot = new Experiment();
        Experiment currentlyArchived = new Experiment();
        currentlyArchived.setArchivedAt(Instant.now());

        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setArchivedAt(null);
        attachment.setExperiment(staleSnapshot);

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));
        when(experimentRepository.findById(any())).thenReturn(Optional.of(currentlyArchived));

        assertThrows(ResourceConflictException.class,
                () -> fileService.archiveFile(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE));

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void archiveFile_ShouldThrowResourceConflict_WhenExperimentIsArchived() {
        UUID fileId = UUID.randomUUID();
        Experiment archivedExperiment = new Experiment();
        archivedExperiment.setArchivedAt(Instant.now());

        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setArchivedAt(null);
        attachment.setExperiment(archivedExperiment);

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        assertThrows(ResourceConflictException.class, () -> fileService.archiveFile(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE));

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void archiveFile_ShouldBeNoOp_WhenFileAlreadyArchived() {
        UUID fileId = UUID.randomUUID();
        Instant originalArchivedAt = Instant.now();

        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setArchivedAt(originalArchivedAt);
        attachment.setExperiment(new Experiment());

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        fileService.archiveFile(fileId, REQUESTING_USER_ID, REQUESTING_USER_ROLE);

        assertEquals(originalArchivedAt, attachment.getArchivedAt(), "Archived timestamp should remain unchanged");
        verify(attachmentRepository, never()).save(any());
    }
}
