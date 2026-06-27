package com.reactur.backend.services;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.RoleDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetUserDto> getAllUsers() {
        log.debug("Fetching all users from the database");
        List<GetUserDto> users = userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        log.info("Successfully retrieved {} users", users.size());
        return users;
    }

    @Override
    @Transactional
    public void setRole(String uuid, RoleDto roleDto) {
        log.debug("Attempting to update role for user ID: {} to {}", uuid, roleDto.role());
        User user = findUserOrThrow(uuid);

        if (user.getRole() != roleDto.role()) {
            user.setRole(roleDto.role());
            userRepository.save(user);
            log.info("Successfully updated role for user ID: {} to {}", uuid, roleDto.role());
        } else {
            log.debug("User ID: {} already has role {}. No DB update required.", uuid, roleDto.role());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRole(String id) {
        log.debug("Fetching role for user ID: {}", id);
        User user = findUserOrThrow(id);
        return new RoleDto(user.getRole());
    }

    @Override
    @Transactional
    public void archiveUser(String uuid) {
        log.debug("Attempting to archive user ID: {}", uuid);
        User user = findUserOrThrow(uuid);

        if (user.getDeactivatedAt() == null) {
            user.setDeactivatedAt(Instant.now());
            userRepository.save(user);
            log.info("Successfully archived user ID: {}", uuid);
        } else {
            log.debug("User ID: {} is already archived. No action taken.", uuid);
        }
    }

    @Override
    @Transactional
    public void restoreUser(String uuid) {
        log.debug("Attempting to restore user ID: {}", uuid);
        User user = findUserOrThrow(uuid);

        if (user.getDeactivatedAt() != null) {
            user.setDeactivatedAt(null);
            userRepository.save(user);
            log.info("Successfully restored user ID: {}", uuid);
        } else {
            log.debug("User ID: {} is already active. No action taken.", uuid);
        }
    }

    // --- Private Helper Methods ---

    private User findUserOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Lookup failed: User with ID {} not found.", id);
                    return new ResourceNotFoundException("User with ID " + id + " not found.");
                });
    }

    private GetUserDto mapToDto(User user) {
        return new GetUserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getDeactivatedAt()
        );
    }
}