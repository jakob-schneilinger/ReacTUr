package com.reactur.backend.mappers;

import com.reactur.backend.dtos.users.GetUserDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toDto_shouldMapAllFields() {
        Instant deactivatedAt = Instant.now();
        User user = User.builder()
                .id("user-123")
                .email("jane.doe@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .role(Role.ADMIN)
                .deactivatedAt(deactivatedAt)
                .build();

        GetUserDto dto = mapper.toDto(user);

        assertEquals(user.getId(), dto.id());
        assertEquals(user.getFirstName(), dto.firstName());
        assertEquals(user.getLastName(), dto.lastName());
        assertEquals(user.getRole(), dto.role());
        assertEquals(deactivatedAt, dto.deactivatedAt());
    }

    @Test
    void toDto_shouldReturnNull_whenUserIsNull() {
        assertNull(mapper.toDto(null));
    }
}
