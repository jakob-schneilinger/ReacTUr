package com.reactur.backend.mappers;

import org.mapstruct.Mapping;

import java.util.List;

public interface BaseMapper<E, T> {

    @Mapping(target = "uuid", source = "id")
    T toDto(E entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    E toEntity(T dto);

    List<T> toDtoList(List<E> entities);
}
