package com.reactur.backend.services;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.SetRoleDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetUserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setRole(String uuid, SetRoleDto setRoleDto) {
        User user = findUserOrThrow(uuid);

        if (user.getRole() != setRoleDto.role()) {
            user.setRole(setRoleDto.role());
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void archiveUser(String uuid) {
        User user = findUserOrThrow(uuid);

        if (user.getDeactivatedAt() == null) {
            user.setDeactivatedAt(Instant.now());
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void restoreUser(String uuid) {
        User user = findUserOrThrow(uuid);

        if (user.getDeactivatedAt() != null) {
            user.setDeactivatedAt(null);
            userRepository.save(user);
        }
    }

    // --- Private Helper Methods ---

    private User findUserOrThrow(String uuid) {
        return userRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + uuid + " not found."));
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