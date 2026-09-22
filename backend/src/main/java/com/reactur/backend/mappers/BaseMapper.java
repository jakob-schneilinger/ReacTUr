package com.reactur.backend.mappers;

import org.mapstruct.Mapping;

import java.util.List;

/**
 * Shared entity/DTO mapping contract for the simple option catalogs (chemical type, instrument,
 * modality, solvent).
 *
 * @param <E> the entity type
 * @param <T> the DTO type
 */
public interface BaseMapper<E, T> {

    @Mapping(target = "uuid", source = "id")
    T toDto(E entity);

    /**
     * {@code id} and {@code archivedAt} are always ignored, even if the DTO carries values for
     * them. Both are server-assigned and must never be taken from client input.
     *
     * @param dto the DTO to map
     * @return the mapped entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    E toEntity(T dto);

    List<T> toDtoList(List<E> entities);
}
