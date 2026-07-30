package com.reactur.backend.mappers;

import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentFolder;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderMapperTest {

    private final FolderMapper mapper = Mappers.getMapper(FolderMapper.class);

    @Test
    void folderToGetFolderDto_shouldMapExperimentsToTheirIds() {
        User owner = User.builder().id("owner-1").build();
        Experiment experimentA = Experiment.builder().id(UUID.randomUUID()).build();
        Experiment experimentB = Experiment.builder().id(UUID.randomUUID()).build();

        ExperimentFolder folder = ExperimentFolder.builder()
                .id(UUID.randomUUID())
                .name("My Folder")
                .owner(owner)
                .experiments(List.of(experimentA, experimentB))
                .build();

        GetFolderDto dto = mapper.folderToGetFolderDto(folder);

        assertEquals(folder.getId(), dto.id());
        assertEquals(folder.getName(), dto.name());
        assertEquals(List.of(experimentA.getId(), experimentB.getId()), dto.experimentIds());
    }

    @Test
    void folderToGetFolderDto_shouldMapEmptyList_whenNoExperiments() {
        ExperimentFolder folder = ExperimentFolder.builder()
                .id(UUID.randomUUID())
                .name("Empty Folder")
                .experiments(List.of())
                .build();

        GetFolderDto dto = mapper.folderToGetFolderDto(folder);

        assertTrue(dto.experimentIds().isEmpty());
    }
}
