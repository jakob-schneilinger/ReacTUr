package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.dtos.folders.SaveFolderDto;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.security.SecurityConfig;
import com.reactur.backend.services.ExperimentFolderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExperimentFolderEndpoint.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(org.springframework.boot.h2console.autoconfigure.H2ConsoleProperties.class)
class ExperimentFolderEndpointTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ExperimentFolderService folderService;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  private final String userId = "mock-keycloak-id-123";

  @Test
  void createFolder_whenValidInput_returns201Created() throws Exception {
    SaveFolderDto requestDto = new SaveFolderDto("My Synthesis");
    UUID newFolderId = UUID.randomUUID();
    GetFolderDto responseDto = new GetFolderDto(newFolderId, "My Synthesis", Collections.emptyList());

    Mockito.when(folderService.createFolder(any(SaveFolderDto.class), Mockito.eq(userId)))
      .thenReturn(responseDto);

    mockMvc.perform(post("/api/folders")
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject(userId)))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestDto)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value(newFolderId.toString()))
      .andExpect(jsonPath("$.name").value("My Synthesis"));
  }

  @Test
  void createFolder_whenNameIsBlank_returns400BadRequest() throws Exception {
    // Validation Test: Name is empty, which violates @NotBlank
    SaveFolderDto invalidRequestDto = new SaveFolderDto("   ");

    mockMvc.perform(post("/api/folders")
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject(userId)))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequestDto)))
      .andExpect(status().isBadRequest());

    Mockito.verifyNoInteractions(folderService);
  }

  @Test
  void getMyFolders_returns200OkWithList() throws Exception {
    UUID folderId = UUID.randomUUID();
    GetFolderDto responseDto = new GetFolderDto(folderId, "My Synthesis", List.of(UUID.randomUUID()));

    Mockito.when(folderService.getMyFolders(userId))
      .thenReturn(List.of(responseDto));

    mockMvc.perform(get("/api/folders")
        .with(jwt().jwt(builder -> builder.subject(userId))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].name").value("My Synthesis"))
      .andExpect(jsonPath("$[0].experimentIds.length()").value(1));
  }

  @Test
  void addExperimentToFolder_returns200Ok() throws Exception {
    UUID folderId = UUID.randomUUID();
    UUID experimentId = UUID.randomUUID();
    GetFolderDto responseDto = new GetFolderDto(folderId, "My Synthesis", List.of(experimentId));

    Mockito.when(folderService.addExperimentToFolder(folderId, experimentId, userId))
      .thenReturn(responseDto);

    mockMvc.perform(post("/api/folders/" + folderId + "/experiments/" + experimentId)
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject(userId))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.experimentIds[0]").value(experimentId.toString()));
  }

  @Test
  void removeExperimentFromFolder_returns200Ok() throws Exception {
    UUID folderId = UUID.randomUUID();
    UUID experimentId = UUID.randomUUID();

    // Response implies the experiment was removed, so the list is now empty
    GetFolderDto responseDto = new GetFolderDto(folderId, "My Synthesis", Collections.emptyList());

    Mockito.when(folderService.removeExperimentFromFolder(folderId, experimentId, userId))
      .thenReturn(responseDto);

    mockMvc.perform(delete("/api/folders/" + folderId + "/experiments/" + experimentId)
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject(userId))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.experimentIds.length()").value(0));
  }

  @Test
  void deleteFolder_returns204NoContent() throws Exception {
    UUID folderId = UUID.randomUUID();

    mockMvc.perform(delete("/api/folders/" + folderId)
        .with(csrf())
        .with(jwt().jwt(builder -> builder.subject(userId))))
      .andExpect(status().isNoContent());

    Mockito.verify(folderService).deleteFolder(folderId, userId);
  }
}
