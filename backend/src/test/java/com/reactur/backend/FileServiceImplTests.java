package com.reactur.backend;

import com.reactur.backend.dtos.ExperimentDtos.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.ExperimentDtos.ExperimentAttachmentDto;
import com.reactur.backend.entities.ExperimentEntities.Experiment;
import com.reactur.backend.entities.ExperimentEntities.ExperimentAttachment;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.ExperimentAttachmentRepository;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.services.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

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
public class FileServiceImplTests {

    @Mock
    private ExperimentAttachmentRepository attachmentRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private FileServiceImpl fileService;

    private final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "bucketName", BUCKET_NAME);
    }

    @Test
    void storeFile_ShouldStoreSuccessfully() {

        UUID experimentId = UUID.randomUUID();
        Experiment experiment = new Experiment(); // Gegebenenfalls Pflichtfelder setzen

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

        ExperimentAttachmentDetailsDto result = fileService.storeFile(experimentId, file);

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
                () -> fileService.storeFile(experimentId, file));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void storeFile_ShouldThrowIllegalArgumentException_WhenFileIsEmpty() {
        UUID experimentId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileService.storeFile(experimentId, emptyFile)
        );

        assertEquals("Cannot upload an empty file.", exception.getMessage());

        verify(experimentRepository, never()).findById(any());
        verify(attachmentRepository, never()).save(any());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void loadFileAsResource_ShouldReturnResource() {
        UUID fileId = UUID.randomUUID();
        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setStorageReferenceId("s3-key-123");

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        ResponseInputStream<GetObjectResponse> mockStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);

        Resource result = fileService.loadFileAsResource(fileId);

        assertNotNull(result);
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void getAttachmentDetail_ShouldReturnDto() {
        UUID fileId = UUID.randomUUID();
        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setId(fileId);
        attachment.setFileName("data.csv");
        attachment.setFileSizeBytes(1024L);

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        ExperimentAttachmentDetailsDto result = fileService.getAttachmentDetail(fileId);

        assertEquals("data.csv", result.fileName());
        assertEquals(1024L, result.fileSizeBytes());
    }

    @Test
    void getAttachmentDetail_ShouldThrowNotFound() {
        UUID fileId = UUID.randomUUID();
        when(attachmentRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fileService.getAttachmentDetail(fileId));
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

        assertThrows(ResourceNotFoundException.class, () -> fileService.archiveFile(fileId));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void archiveFile_ShouldArchiveSuccessfully_WhenFileIsActive() {
        UUID fileId = UUID.randomUUID();
        ExperimentAttachment attachment = new ExperimentAttachment();
        attachment.setArchivedAt(null);

        when(attachmentRepository.findById(fileId)).thenReturn(Optional.of(attachment));

        fileService.archiveFile(fileId);

        verify(attachmentRepository, times(1)).save(attachment);
    }
}
