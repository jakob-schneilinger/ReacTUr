package com.reactur.backend.mappers;

import org.mapstruct.Mapping;

import java.util.List;

public interface BaseMapper<E, T> {

    // TODO-CLAUDE: Mapper method naming is currently inconsistent across the codebase and should be
    // unified in a future dedicated rename pass (this touches call sites in services/endpoints/tests,
    // so it's out of scope for a mechanical cleanup):
    //   - Short `toDto`/`toEntity`/`toDetailDto` style: BaseMapper (and its four options mappers:
    //     InstrumentMapper, SolventMapper, ModalityMapper, ChemicalTypeMapper), ReactantMapper,
    //     UserMapper, StructureMapper.
    //   - Verbose `xToYDto` style: ExperimentMapper (experimentToGetExperimentDto), FolderMapper
    //     (folderToGetFolderDto), MetadataMapper (metadataToGetMetadataDto), MeasurementDataMapper
    //     (resultToMeasurementDataDto).
    // The team should pick one convention going forward.

    @Mapping(target = "uuid", source = "id")
    T toDto(E entity);

    @Mapping(target = "id", source = "uuid")
    E toEntity(T dto);

    List<T> toDtoList(List<E> entities);
}
