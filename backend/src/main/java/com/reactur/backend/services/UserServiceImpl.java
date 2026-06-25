package com.reactur.backend.services;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.SetRoleDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakId = jwt.getSubject();
            return userRepository.findById(keycloakId);
        }
        return Optional.empty();
    }

    public List<GetUserDto> getAllUsers() {
        return null;
    }

    @Override
    public void setRole(String id, SetRoleDto setRoleDto) {

    }

    @Override
    public void archiveUser(String id) {

    }

    @Override
    public void restoreUser(String id) {

    }
}