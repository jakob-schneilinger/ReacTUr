package com.reactur.backend.services;

import com.reactur.backend.dtos.users.GetUserDto;
import com.reactur.backend.dtos.users.RoleDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exceptions.ResourceConflictException;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import com.reactur.backend.mappers.UserMapper;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.implementations.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    private User mockUser;
    private final String STRING_ID = "User1";

    @BeforeEach
    void setUp() {
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);
        userService = new UserServiceImpl(userRepository, userMapper);

        mockUser = new User();
        mockUser.setId(STRING_ID);
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");
        mockUser.setRole(Role.USER);
        mockUser.setDeactivatedAt(null);
    }

    // --- GET ALL USERS ---

    @Test
    void getAllUsers_ShouldReturnMappedDtos() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser));

        List<GetUserDto> result = userService.getAllUsers();

        assertFalse(result.isEmpty());
        assertEquals(STRING_ID, result.getFirst().id());
        assertEquals("John", result.getFirst().firstName());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_ShouldReturnEmptyList_WhenNoUsersExist() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<GetUserDto> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    // --- SET ROLE ---

    @Test
    void setRole_ShouldUpdateRole_WhenUserExists() {
        RoleDto dto = new RoleDto(Role.ADMIN);
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));

        userService.setRole(STRING_ID, dto);

        assertEquals(Role.ADMIN, mockUser.getRole());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void setRole_ShouldNotSave_WhenRoleIsAlreadySet() {
        RoleDto dto = new RoleDto(Role.USER);
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));

        userService.setRole(STRING_ID, dto);

        verify(userRepository, never()).save(any());
    }

    @Test
    void setRole_ShouldThrowNotFound_WhenUserDoesNotExist() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.setRole(STRING_ID, new RoleDto(Role.ADMIN)));
    }

    @Test
    void setRole_ShouldThrowConflict_WhenDemotingTheLastAdmin() {
        mockUser.setRole(Role.ADMIN);
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(ResourceConflictException.class,
                () -> userService.setRole(STRING_ID, new RoleDto(Role.USER)));

        assertEquals(Role.ADMIN, mockUser.getRole()); // Unchanged
        verify(userRepository, never()).save(any());
    }

    @Test
    void setRole_ShouldAllowDemotingAnAdmin_WhenAnotherAdminExists() {
        mockUser.setRole(Role.ADMIN);
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);

        userService.setRole(STRING_ID, new RoleDto(Role.USER));

        assertEquals(Role.USER, mockUser.getRole());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void setRole_ShouldNotCheckAdminCount_WhenPromotingToAdmin() {
        // user starts as Role.USER (see setUp) - promoting them to ADMIN can never reduce the
        // admin count, so countByRole must not even be consulted.
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));

        userService.setRole(STRING_ID, new RoleDto(Role.ADMIN));

        assertEquals(Role.ADMIN, mockUser.getRole());
        verify(userRepository, never()).countByRole(any());
    }

    // --- GET ROLE ---

    @Test
    void getRole_ShouldReturnRole_WhenUserExists() {
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));

        RoleDto result = userService.getRole(STRING_ID);

        assertNotNull(result);
        assertEquals(Role.USER, result.role());
        verify(userRepository, times(1)).findById(STRING_ID);
    }

    @Test
    void getRole_ShouldThrowNotFound_WhenUserDoesNotExist() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getRole(STRING_ID));
    }

    // --- ARCHIVE USER ---

    @Test
    void archiveUser_ShouldSetDeactivatedAt_WhenUserIsActive() {
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));

        userService.archiveUser(STRING_ID);

        assertNotNull(mockUser.getDeactivatedAt());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void archiveUser_ShouldNotUpdateTimestamp_WhenUserIsAlreadyArchived() {
        Instant pastTime = Instant.now().minusSeconds(3600);
        mockUser.setDeactivatedAt(pastTime);
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));

        userService.archiveUser(STRING_ID);

        assertEquals(pastTime, mockUser.getDeactivatedAt()); // Timestamp should not change
        verify(userRepository, never()).save(any());
    }

    @Test
    void archiveUser_ShouldThrowNotFound_WhenUserDoesNotExist() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.archiveUser(STRING_ID));
    }

    // --- RESTORE USER ---

    @Test
    void restoreUser_ShouldNullifyDeactivatedAt_WhenUserIsArchived() {
        mockUser.setDeactivatedAt(Instant.now());
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));

        userService.restoreUser(STRING_ID);

        assertNull(mockUser.getDeactivatedAt());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void restoreUser_ShouldNotSave_WhenUserIsAlreadyActive() {
        when(userRepository.findById(STRING_ID)).thenReturn(Optional.of(mockUser));

        userService.restoreUser(STRING_ID);

        assertNull(mockUser.getDeactivatedAt());
        verify(userRepository, never()).save(any());
    }

    @Test
    void restoreUser_ShouldThrowNotFound_WhenUserDoesNotExist() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.restoreUser(STRING_ID));
    }
}
