package com.reactur.backend.dtos;

import com.reactur.backend.enums.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a system user.
 *
 * @param id            The unique identifier of the user.
 * @param firstName     The user's first name.
 * @param lastName      The user's last name.
 * @param role          The authorization role assigned to the user (e.g., ADMIN, USER).
 * @param deactivatedAt The timestamp when the user's account was deactivated (null if active).
 */
public record GetUserDto(
        String id,
        String firstName,
        String lastName,
        Role role,
        Instant deactivatedAt
) {
}
