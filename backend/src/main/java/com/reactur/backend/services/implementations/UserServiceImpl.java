package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.RoleDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.UserMapper;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GetUserDto> getAllUsers() {
        log.debug("Fetching all users from the database");
        List<GetUserDto> users = userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
        log.info("Successfully retrieved {} users", users.size());
        return users;
    }

    @Override
    @Transactional
    public void setRole(String id, RoleDto roleDto) {
        log.debug("Attempting to update role for user ID: {} to {}", id, roleDto.role());
        User user = findUserOrThrow(id);

        if (user.getRole() == roleDto.role()) {
            log.debug("User ID: {} already has role {}. No DB update required.", id, roleDto.role());
            return;
        }

        if (isDemotingTheLastAdmin(user, roleDto.role())) {
            throw new ResourceConflictException(
                    "Cannot change this user's role: they are the last remaining ADMIN. Promote another user to ADMIN first.");
        }

        user.setRole(roleDto.role());
        userRepository.save(user);
        log.info("Successfully updated role for user ID: {} to {}", id, roleDto.role());
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
    public void archiveUser(String id) {
        log.debug("Attempting to archive user ID: {}", id);
        User user = findUserOrThrow(id);

        if (user.getDeactivatedAt() == null) {
            user.setDeactivatedAt(Instant.now());
            userRepository.save(user);
            log.info("Successfully archived user ID: {}", id);
        } else {
            log.debug("User ID: {} is already archived. No action taken.", id);
        }
    }

    @Override
    @Transactional
    public void restoreUser(String id) {
        log.debug("Attempting to restore user ID: {}", id);
        User user = findUserOrThrow(id);

        if (user.getDeactivatedAt() != null) {
            user.setDeactivatedAt(null);
            userRepository.save(user);
            log.info("Successfully restored user ID: {}", id);
        } else {
            log.debug("User ID: {} is already active. No action taken.", id);
        }
    }

    // --- Private Helper Methods ---

    /**
     * True if {@code user} currently holds the ADMIN role, the requested {@code newRole} would
     * take it away, and no other ADMIN account exists to fall back on - which would otherwise
     * leave the system with zero admins and no in-app way to recover.
     */
    private boolean isDemotingTheLastAdmin(User user, Role newRole) {
        return user.getRole() == Role.ADMIN
                && newRole != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1;
    }

    private User findUserOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found."));
    }

}
