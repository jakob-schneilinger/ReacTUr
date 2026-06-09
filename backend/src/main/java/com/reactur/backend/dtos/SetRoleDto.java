package com.reactur.backend.dtos;

import com.reactur.backend.enums.Role;

import java.util.UUID;

/**
 * DTO for assigning or updating a user's role.
 *
 * @param uuid The unique identifier of the user to update.
 * @param role The new role to assign to the user.
 */
public record SetRoleDto(UUID uuid, Role role) {
}
