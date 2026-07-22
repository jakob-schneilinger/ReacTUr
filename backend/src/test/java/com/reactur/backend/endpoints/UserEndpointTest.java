package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.RoleDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.GlobalExceptionHandler;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.security.SecurityConfig;
import com.reactur.backend.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserEndpoint.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getAllUsers_WhenAdmin_ShouldReturn200AndList() throws Exception {
        String id = "some-id-lol";
        GetUserDto mockResponse = new GetUserDto(id, "Jane", "Doe", Role.ADMIN, null);

        when(userService.getAllUsers()).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void getAllUsers_WhenSupervisor_ShouldReturn200AndList() throws Exception {
        String id = "some-id-lol";
        GetUserDto mockResponse = new GetUserDto(id, "Jane", "Doe", Role.ADMIN, null);

        when(userService.getAllUsers()).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void getAllUsers_WhenUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }

    @Test
    void setRole_WhenAdmin_ShouldReturn200_WhenSuccessful() throws Exception {
        String id = "some-id-lol";
        RoleDto dto = new RoleDto(Role.SUPERVISOR);

        mockMvc.perform(post("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    // SUPERVISOR must never add/modify users - setRole stays ADMIN-only,
    // pinning against a future "extend everything to SUPERVISOR" regression.
    @Test
    void setRole_WhenSupervisor_ShouldReturn403() throws Exception {
        String id = "some-id-lol";
        RoleDto dto = new RoleDto(Role.SUPERVISOR);

        mockMvc.perform(post("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).setRole(any(), any());
    }

    @Test
    void setRole_WhenUser_ShouldReturn403() throws Exception {
        String id = "some-id-lol";
        RoleDto dto = new RoleDto(Role.SUPERVISOR);

        mockMvc.perform(post("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).setRole(any(), any());
    }

    @Test
    void getRole_ShouldReturn200AndRole_WhenUserExists() throws Exception {
        String id = "some-id-lol";
        RoleDto mockResponse = new RoleDto(Role.SUPERVISOR);

        when(userService.getRole(id)).thenReturn(mockResponse);

        // Self-lookup: no role/authority required, only that the JWT subject matches the
        // requested id (see the comment on UserEndpoint#getRole and SecurityConfig's exemption).
        mockMvc.perform(get("/api/users/" + id + "/role")
                        .with(jwt().jwt(builder -> builder.subject(id))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    @Test
    void getRole_ShouldReturn404_WhenUserNotFound() throws Exception {
        String id = "some-id-lol";

        when(userService.getRole(id)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/" + id + "/role")
                        .with(jwt().jwt(builder -> builder.subject(id))))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveUser_WhenAdmin_ShouldReturn200_WhenSuccessful() throws Exception {
        String id = "some-id-lol";

        mockMvc.perform(delete("/api/users/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    // SUPERVISOR must never add/modify users - archiveUser stays
    // ADMIN-only, pinning against a future "extend everything to SUPERVISOR" regression.
    @Test
    void archiveUser_WhenSupervisor_ShouldReturn403() throws Exception {
        String id = "some-id-lol";

        mockMvc.perform(delete("/api/users/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).archiveUser(any());
    }

    @Test
    void archiveUser_WhenUser_ShouldReturn403() throws Exception {
        String id = "some-id-lol";

        mockMvc.perform(delete("/api/users/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).archiveUser(any());
    }

    @Test
    void restoreUser_WhenAdmin_ShouldReturn404_WhenUserNotFound() throws Exception {
        String id = "some-id-lol";

        doThrow(new ResourceNotFoundException("User not found")).when(userService).restoreUser(id);

        mockMvc.perform(put("/api/users/" + id + "/restore")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    // SUPERVISOR must never add/modify users - restoreUser stays
    // ADMIN-only, pinning against a future "extend everything to SUPERVISOR" regression.
    @Test
    void restoreUser_WhenSupervisor_ShouldReturn403() throws Exception {
        String id = "some-id-lol";

        mockMvc.perform(put("/api/users/" + id + "/restore")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).restoreUser(any());
    }

    @Test
    void restoreUser_WhenUser_ShouldReturn403() throws Exception {
        String id = "some-id-lol";

        mockMvc.perform(put("/api/users/" + id + "/restore")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).restoreUser(any());
    }
}
