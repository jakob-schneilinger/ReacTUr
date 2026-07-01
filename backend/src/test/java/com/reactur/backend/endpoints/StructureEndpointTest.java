package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.ReactantDtos.StructureDto;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.security.SecurityConfig;
import com.reactur.backend.services.StructureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

@WebMvcTest(StructureEndpoint.class)
@Import(SecurityConfig.class)
public class StructureEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StructureService structureService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void uploadStructure_whenAdmin_returns200Ok() throws Exception {
        UUID reactantId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xyz",
                MediaType.TEXT_PLAIN_VALUE,
                "XYZ content".getBytes()
        );

        StructureDto mockDto = new StructureDto(UUID.randomUUID(), reactantId, "test.xyz", "XYZ content", StructureFlag.READY);
        when(structureService.uploadStructure(eq(reactantId), any())).thenReturn(mockDto);

        mockMvc.perform(multipart(HttpMethod.POST, "/api/structure/{reactantId}", reactantId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mockDto.id().toString()))
                .andExpect(jsonPath("$.name").value("test.xyz"))
                .andExpect(jsonPath("$.flag").value("READY"));

        verify(structureService, times(1)).uploadStructure(eq(reactantId), any());
    }

    @Test
    void uploadStructure_whenUser_returns403Forbidden() throws Exception {
        UUID reactantId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.xyz", MediaType.TEXT_PLAIN_VALUE, "XYZ".getBytes());

        mockMvc.perform(multipart(HttpMethod.POST, "/api/structure/{reactantId}", reactantId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(structureService, never()).uploadStructure(any(), any());
    }

    @Test
    void generateStructureFromAPI_whenAdmin_returns204NoContent() throws Exception {
        UUID reactantId = UUID.randomUUID();

        doNothing().when(structureService).generateStructureFromAPI(reactantId);

        mockMvc.perform(post("/api/structure/{reactantId}/generate", reactantId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(structureService, times(1)).generateStructureFromAPI(reactantId);
    }

    @Test
    void generateStructureFromAPI_whenUser_returns403Forbidden() throws Exception {
        UUID reactantId = UUID.randomUUID();

        mockMvc.perform(post("/api/structure/{reactantId}/generate", reactantId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(structureService, never()).generateStructureFromAPI(any());
    }

    @Test
    void selectStructure_whenAdmin_returns200Ok() throws Exception {
        UUID structureId = UUID.randomUUID();
        StructureDto mockDto = new StructureDto(structureId, UUID.randomUUID(), "active_struct", "XYZ content", StructureFlag.ACTIVE);

        when(structureService.selectStructure(structureId)).thenReturn(mockDto);

        mockMvc.perform(post("/api/structure/{structureId}/select", structureId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(structureId.toString()))
                .andExpect(jsonPath("$.flag").value("ACTIVE"));

        verify(structureService, times(1)).selectStructure(structureId);
    }

    @Test
    void selectStructure_whenUser_returns403Forbidden() throws Exception {
        UUID structureId = UUID.randomUUID();

        mockMvc.perform(post("/api/structure/{structureId}/select", structureId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(structureService, never()).selectStructure(any());
    }

    @Test
    void anyEndpoint_whenUnauthenticated_returns401() throws Exception {
        UUID structureId = UUID.randomUUID();

        mockMvc.perform(post("/api/structure/{structureId}/select", structureId))
                .andExpect(status().isUnauthorized());
    }
}
