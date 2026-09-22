package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.users.GetUserDto;
import com.reactur.backend.dtos.users.RoleDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exceptions.ResourceConflictException;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import com.reactur.backend.mappers.UserMapper;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

        // If someone else changes this same user's role first, this save fails loudly instead of silently
        // overwriting it. That's a different (and separate) problem from the last-admin check above, which
        // has its own still-open gap.
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
            // Two archive requests at the same time would normally conflict even though they both want the
            // same outcome. Treat that as "someone already archived it" instead of an error.
            try {
                userRepository.save(user);
                log.info("Successfully archived user ID: {}", id);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.debug("User {} was archived concurrently by another request; treating as already archived.", id);
            }
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
            // Same idea as archiveUser above.
            try {
                userRepository.save(user);
                log.info("Successfully restored user ID: {}", id);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.debug("User {} was restored concurrently by another request; treating as already restored.", id);
            }
        } else {
            log.debug("User ID: {} is already active. No action taken.", id);
        }
    }

    // --- Private Helper Methods ---

    // TODO known gap, not fixed: this check counts admins across the whole table instead of locking one
    // specific row. With exactly 2 admins, two people could each demote the OTHER admin at the same
    // time, both see "2 admins" as they check, and both proceed. That leaves zero admins with no way
    // to recover in the app.
    /**
     * True if {@code user} currently holds the ADMIN role, the requested {@code newRole} would
     * take it away, and no other ADMIN account exists to fall back on. Allowing this would leave
     * the system with zero admins and no in-app way to recover.
     *
     * @param user    the user whose role is being changed
     * @param newRole the role being assigned
     * @return true if this change would demote the last remaining admin
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
