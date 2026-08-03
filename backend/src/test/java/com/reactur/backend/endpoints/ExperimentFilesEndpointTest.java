package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.GlobalExceptionHandler;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.security.RoleExtractor;
import com.reactur.backend.security.SecurityConfig;
import com.reactur.backend.services.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExperimentFilesEndpoint.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, RoleExtractor.class})
class ExperimentFilesEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor approvedUser(String subject) {
        return jwt().jwt(builder -> builder.subject(subject))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void uploadFile_ShouldReturn201_WhenUploadIsSuccessful() throws Exception {
        UUID experimentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "Dummy PDF Content".getBytes());

        ExperimentAttachmentDetailsDto mockResponse = new ExperimentAttachmentDetailsDto(
                UUID.randomUUID(), "test.pdf", MediaType.APPLICATION_PDF_VALUE, 1024L, "checksum123", "/api/file/123"
        );

        when(fileService.storeFile(eq(experimentId), any(), any(), any(Role.class))).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/file/" + experimentId).file(file)
                        .with(approvedUser("uploader-user-id")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("test.pdf"));
    }

    @Test
    void uploadFile_ShouldReturn404_WhenExperimentNotFound() throws Exception {
        UUID experimentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "Dummy PDF Content".getBytes());

        when(fileService.storeFile(eq(experimentId), any(), any(), any(Role.class)))
                .thenThrow(new ResourceNotFoundException("Experiment not found"));

        mockMvc.perform(multipart("/api/file/" + experimentId).file(file)
                        .with(approvedUser("uploader-user-id")))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadFile_ShouldReturn403_WhenUserCannotModifyExperiment() throws Exception {
        UUID experimentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "Dummy PDF Content".getBytes());

        when(fileService.storeFile(eq(experimentId), any(), any(), any(Role.class)))
                .thenThrow(new AccessDeniedException("You do not have permission to modify this experiment."));

        mockMvc.perform(multipart("/api/file/" + experimentId).file(file)
                        .with(approvedUser("non-owner-user-id")))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadFile_ShouldReturnResourceWithCorrectHeaders() throws Exception {
        UUID fileId = UUID.randomUUID();

        ExperimentAttachmentDetailsDto mockDetails = new ExperimentAttachmentDetailsDto(
                fileId, "data.csv", "text/csv", 500L, "checksum", "/api/file/" + fileId
        );
        Resource mockResource = new ByteArrayResource("CSV Data".getBytes());

        when(fileService.getAttachmentDetail(eq(fileId), any(), any(Role.class))).thenReturn(mockDetails);
        when(fileService.loadFileAsResource(eq(fileId), any(), any(Role.class))).thenReturn(mockResource);

        mockMvc.perform(get("/api/file/" + fileId)
                        .with(approvedUser("viewer-user-id")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string("CSV Data"));
    }

    @Test
    void downloadFile_ShouldEscapeEmbeddedQuote_WhenFileNameContainsOne() throws Exception {
        UUID fileId = UUID.randomUUID();
        String fileName = "the \"best\" run.csv";

        ExperimentAttachmentDetailsDto mockDetails = new ExperimentAttachmentDetailsDto(
                fileId, fileName, "text/csv", 500L, "checksum", "/api/file/" + fileId
        );
        Resource mockResource = new ByteArrayResource("CSV Data".getBytes());

        when(fileService.getAttachmentDetail(eq(fileId), any(), any(Role.class))).thenReturn(mockDetails);
        when(fileService.loadFileAsResource(eq(fileId), any(), any(Role.class))).thenReturn(mockResource);

        mockMvc.perform(get("/api/file/" + fileId)
                        .with(approvedUser("viewer-user-id")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"the \\\"best\\\" run.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string("CSV Data"));
    }

    @Test
    void downloadFile_ShouldReturn404_WhenFileNotFound() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileService.getAttachmentDetail(eq(fileId), any(), any(Role.class)))
                .thenThrow(new ResourceNotFoundException("File not found"));

        mockMvc.perform(get("/api/file/" + fileId)
                        .with(approvedUser("viewer-user-id")))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadFile_ShouldReturn403_WhenUserCannotViewExperiment() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileService.getAttachmentDetail(eq(fileId), any(), any(Role.class)))
                .thenThrow(new AccessDeniedException("You do not have permission to view this experiment."));

        mockMvc.perform(get("/api/file/" + fileId)
                        .with(approvedUser("non-owner-user-id")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFileDetails_ShouldReturn200_WhenFileExists() throws Exception {
        UUID fileId = UUID.randomUUID();
        ExperimentAttachmentDetailsDto mockResponse = new ExperimentAttachmentDetailsDto(
                fileId, "image.png", MediaType.IMAGE_PNG_VALUE, 2048L, "checksum", "/api/file/" + fileId
        );

        when(fileService.getAttachmentDetail(eq(fileId), any(), any(Role.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/file/" + fileId + "/details")
                        .with(approvedUser("viewer-user-id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("image.png"));
    }

    @Test
    void getFileDetails_ShouldReturn404_WhenFileNotFound() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileService.getAttachmentDetail(eq(fileId), any(), any(Role.class)))
                .thenThrow(new ResourceNotFoundException("File not found"));

        mockMvc.perform(get("/api/file/" + fileId + "/details")
                        .with(approvedUser("viewer-user-id")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFileDetails_ShouldReturn403_WhenUserCannotViewExperiment() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileService.getAttachmentDetail(eq(fileId), any(), any(Role.class)))
                .thenThrow(new AccessDeniedException("You do not have permission to view this experiment."));

        mockMvc.perform(get("/api/file/" + fileId + "/details")
                        .with(approvedUser("non-owner-user-id")))
                .andExpect(status().isForbidden());
    }

    @Test
    void archiveFile_ShouldReturn204_WhenSuccessful() throws Exception {
        UUID fileId = UUID.randomUUID();

        doNothing().when(fileService).archiveFile(eq(fileId), any(), any(Role.class));

        mockMvc.perform(delete("/api/file/" + fileId)
                        .with(approvedUser("deleter-user-id")))
                .andExpect(status().isNoContent());
    }

    @Test
    void archiveFile_ShouldReturn404_WhenFileNotFound() throws Exception {
        UUID fileId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("File not found"))
                .when(fileService).archiveFile(eq(fileId), any(), any(Role.class));

        mockMvc.perform(delete("/api/file/" + fileId)
                        .with(approvedUser("deleter-user-id")))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveFile_ShouldReturn403_WhenUserCannotModifyExperiment() throws Exception {
        UUID fileId = UUID.randomUUID();

        doThrow(new AccessDeniedException("You do not have permission to modify this experiment."))
                .when(fileService).archiveFile(eq(fileId), any(), any(Role.class));

        mockMvc.perform(delete("/api/file/" + fileId)
                        .with(approvedUser("non-owner-user-id")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyEndpoint_whenUnauthenticated_returns401() throws Exception {
        // Mirrors StructureEndpointTest/ChemicalTypeEndpointTest's pattern: a plain request with no
        // JWT at all must be rejected by SecurityConfig before it ever reaches the controller.
        UUID fileId = UUID.randomUUID();

        mockMvc.perform(get("/api/file/" + fileId + "/details"))
                .andExpect(status().isUnauthorized());
    }
}
