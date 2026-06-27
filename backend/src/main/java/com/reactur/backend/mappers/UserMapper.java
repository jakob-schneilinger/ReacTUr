package com.reactur.backend.mappers;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    GetUserDto toDto(User user);
}
