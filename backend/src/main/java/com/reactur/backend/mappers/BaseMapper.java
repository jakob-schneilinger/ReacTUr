package com.reactur.backend.mappers;

import java.util.List;

public interface BaseMapper<E, T> {
    T toDto(E entity);
    E toEntity(T dto);

    List<T> toDtoList(List<E> entities);
    List<E> toEntityList(List<T> dtos);
}
