package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.RoleDto;
import com.reactur.backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserEndpoint {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<GetUserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setRole(
            @PathVariable String id,
            @Valid @RequestBody RoleDto setRole) {

        userService.setRole(id, setRole);
        return ResponseEntity.ok().build();
    }

    // Anyone can check their OWN role (this is how the frontend detects "pending approval",
    // AuthService.getLoggedInUser) so it must keep working even for UNAUTHORIZED users, which is
    // why this path is also exempted from the role-based gate in SecurityConfig). Checking someone
    // ELSE's role requires ADMIN.
    // TODO-CLAUDE (future thread, minor/non-blocking): UserEndpointTest only covers the
    // self-lookup success/404 paths for getRole. Missing test cases: ADMIN checking someone
    // ELSE's role (should be 200), and a non-admin checking someone else's role (should be 403).
    // The annotation below is correct and untouched - this is purely a missing-test-case gap
    // noticed while verifying the SUPERVISOR permission rollout, not a behavior bug.
    @GetMapping("/{id}/role")
    @PreAuthorize("#id == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<RoleDto> getRole(@PathVariable String id) {
        return ResponseEntity.ok(userService.getRole(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archiveUser(@PathVariable String id) {
        userService.archiveUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restoreUser(@PathVariable String id) {
        userService.restoreUser(id);
        return ResponseEntity.ok().build();
    }
}
