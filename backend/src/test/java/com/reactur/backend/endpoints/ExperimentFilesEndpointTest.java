package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.ExperimentDtos.ExperimentAttachmentDetailsDto;
import com.reactur.backend.exeptions.GlobalExceptionHandler;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ExperimentFilesEndpoint.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(GlobalExceptionHandler.class)
class ExperimentFilesEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void uploadFile_ShouldReturn201_WhenUploadIsSuccessful() throws Exception {
        UUID experimentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "Dummy PDF Content".getBytes());

        ExperimentAttachmentDetailsDto mockResponse = new ExperimentAttachmentDetailsDto(
                UUID.randomUUID(), "test.pdf", MediaType.APPLICATION_PDF_VALUE, 1024L, "checksum123", "/api/file/123"
        );

        when(fileService.storeFile(eq(experimentId), any())).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/file/" + experimentId).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("test.pdf"));
    }

    @Test
    void downloadFile_ShouldReturnResourceWithCorrectHeaders() throws Exception {
        UUID fileId = UUID.randomUUID();

        ExperimentAttachmentDetailsDto mockDetails = new ExperimentAttachmentDetailsDto(
                fileId, "data.csv", "text/csv", 500L, "checksum", "/api/file/" + fileId
        );
        Resource mockResource = new ByteArrayResource("CSV Data".getBytes());

        when(fileService.getAttachmentDetail(fileId)).thenReturn(mockDetails);
        when(fileService.loadFileAsResource(fileId)).thenReturn(mockResource);

        mockMvc.perform(get("/api/file/" + fileId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string("CSV Data"));
    }

    @Test
    void getFileDetails_ShouldReturn200_WhenFileExists() throws Exception {
        UUID fileId = UUID.randomUUID();
        ExperimentAttachmentDetailsDto mockResponse = new ExperimentAttachmentDetailsDto(
                fileId, "image.png", MediaType.IMAGE_PNG_VALUE, 2048L, "checksum", "/api/file/" + fileId
        );

        when(fileService.getAttachmentDetail(fileId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/file/" + fileId + "/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("image.png"));
    }

    @Test
    void getFileDetails_ShouldReturn404_WhenFileNotFound() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileService.getAttachmentDetail(fileId))
                .thenThrow(new ResourceNotFoundException("File not found"));

        mockMvc.perform(get("/api/file/" + fileId + "/details"))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveFile_ShouldReturn204_WhenSuccessful() throws Exception {
        UUID fileId = UUID.randomUUID();

        doNothing().when(fileService).archiveFile(fileId);

        mockMvc.perform(delete("/api/file/" + fileId))
                .andExpect(status().isNoContent());
    }
}
