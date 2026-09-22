package com.reactur.backend.mappers;

import com.reactur.backend.dtos.users.GetUserDto;
import com.reactur.backend.entities.User;
import org.mapstruct.Mapper;

/** Maps {@link User} to {@link GetUserDto}. Deliberately excludes email; it's never exposed to clients. */
@Mapper(componentModel = "spring")
public interface UserMapper {

    GetUserDto toDto(User user);
}
