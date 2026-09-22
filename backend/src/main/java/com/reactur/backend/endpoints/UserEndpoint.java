package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.users.GetUserDto;
import com.reactur.backend.dtos.users.RoleDto;
import com.reactur.backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing application users and their roles.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserEndpoint {

    private final UserService userService;

    /**
     * Lists every user in the system. Only ADMIN and SUPERVISOR may list all users.
     *
     * @param jwt the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing every {@link GetUserDto}
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<GetUserDto>> getAllUsers(@AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/users called by user {}", jwt.getSubject());
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Changes a user's role. Only ADMIN may change roles.
     *
     * @param id      the Keycloak ID of the user whose role is being changed
     * @param setRole the new role to assign
     * @param jwt     the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} with a 200 OK status
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setRole(
            @PathVariable String id,
            @Valid @RequestBody RoleDto setRole,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("POST /api/users/{} called by user {}. NewRole: {}", id, jwt.getSubject(), setRole.role());
        userService.setRole(id, setRole);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a user's current role. Anyone can check their own role, since the frontend needs
     * this to detect a still-pending account approval, so it must keep working even for
     * unapproved callers. Checking someone else's role requires ADMIN.
     *
     * @param id  the Keycloak ID of the user whose role is being requested
     * @param jwt the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} containing the user's {@link RoleDto}
     */
    @GetMapping("/{id}/role")
    @PreAuthorize("#id == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<RoleDto> getRole(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/users/{}/role called by user {}", id, jwt.getSubject());
        return ResponseEntity.ok(userService.getRole(id));
    }

    /**
     * Deactivates a user, revoking their access on their next request without deleting their
     * record. Only ADMIN may deactivate a user.
     *
     * @param id  the Keycloak ID of the user to deactivate
     * @param jwt the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} with a 200 OK status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archiveUser(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        log.info("DELETE /api/users/{} called by user {}", id, jwt.getSubject());
        userService.archiveUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Reactivates a previously deactivated user, restoring their stored role. Only ADMIN may
     * restore a user.
     *
     * @param id  the Keycloak ID of the user to restore
     * @param jwt the Keycloak token containing the requesting user's internal ID
     * @return a {@link ResponseEntity} with a 200 OK status
     */
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restoreUser(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /api/users/{}/restore called by user {}", id, jwt.getSubject());
        userService.restoreUser(id);
        return ResponseEntity.ok().build();
    }
}
