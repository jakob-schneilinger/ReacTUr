package com.reactur.backend.mappers;

import org.mapstruct.Mapping;

import java.util.List;

public interface BaseMapper<E, T> {

    @Mapping(source = "id", target = "uuid")
    T toDto(E entity);

    @Mapping(source = "uuid", target = "id")
    E toEntity(T dto);

    List<T> toDtoList(List<E> entities);
    List<E> toEntityList(List<T> dtos);
}
