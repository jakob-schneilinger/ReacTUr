package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.RoleDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.GlobalExceptionHandler;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserEndpoint.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(GlobalExceptionHandler.class)
class UserEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getAllUsers_ShouldReturn200AndList() throws Exception {
        String id = "some-id-lol";
        GetUserDto mockResponse = new GetUserDto(id, "Jane", "Doe", Role.ADMIN, null);

        when(userService.getAllUsers()).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void setRole_ShouldReturn200_WhenSuccessful() throws Exception {
        String id = "some-id-lol";
        RoleDto dto = new RoleDto(Role.SUPERVISOR);

        mockMvc.perform(post("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void getRole_ShouldReturn200AndRole_WhenUserExists() throws Exception {
        String id = "some-id-lol";
        RoleDto mockResponse = new RoleDto(Role.SUPERVISOR);

        when(userService.getRole(id)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/users/" + id + "/role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    @Test
    void getRole_ShouldReturn404_WhenUserNotFound() throws Exception {
        String id = "some-id-lol";

        when(userService.getRole(id)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/" + id + "/role"))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveUser_ShouldReturn200_WhenSuccessful() throws Exception {
        String id = "some-id-lol";

        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void restoreUser_ShouldReturn404_WhenUserNotFound() throws Exception {
        String id = "some-id-lol";

        doThrow(new ResourceNotFoundException("User not found")).when(userService).restoreUser(id);

        mockMvc.perform(put("/api/users/" + id + "/restore"))
                .andExpect(status().isNotFound());
    }
}