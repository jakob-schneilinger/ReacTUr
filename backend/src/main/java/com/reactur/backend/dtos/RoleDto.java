package com.reactur.backend.dtos;

import com.reactur.backend.enums.Role;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for assigning or updating a user's role.
 *
 * @param role The new role to assign to the user.
 */
public record RoleDto(
        @NotNull(message = "Role must not be null")
        Role role) {
}
