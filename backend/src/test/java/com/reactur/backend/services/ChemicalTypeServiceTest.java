package com.reactur.backend.services;

import com.reactur.backend.dtos.options.ChemicalTypeDto;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.options.ChemicalTypeMapper;
import com.reactur.backend.repositories.options.ChemicalTypeRepository;
import com.reactur.backend.services.options.impementations.ChemicalTypeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChemicalTypeServiceTest {

    @Mock
    private ChemicalTypeRepository repository;

    @Mock
    private ChemicalTypeMapper mapper;

    @InjectMocks
    private ChemicalTypeServiceImpl service;

    private final UUID VALID_UUID = UUID.randomUUID();
    private ChemicalType mockEntity;
    private ChemicalTypeDto mockDto;

    @BeforeEach
    void setUp() {
        mockEntity = ChemicalType.builder()
                .id(VALID_UUID)
                .name("Solvent")
                .archivedAt(null)
                .build();

        mockDto = new ChemicalTypeDto(VALID_UUID, "Solvent", null);
    }

    @Test
    void getKnownOptions_ShouldReturnList_WhenOptionsExist() {
        when(repository.findAll()).thenReturn(List.of(mockEntity));
        when(mapper.toDtoList(List.of(mockEntity))).thenReturn(List.of(mockDto));

        List<ChemicalTypeDto> result = service.getKnownOptions();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Solvent", result.getFirst().name());
        verify(repository, times(1)).findAll();
    }

    @Test
    void createOptions_ShouldSaveAndReturnUpdatedList_WhenValidData() {
        ChemicalTypeDto createDto = new ChemicalTypeDto(null, "New Solvent", null);
        ChemicalType entityToSave = ChemicalType.builder().name("New Solvent").build();
        ChemicalType savedEntity = ChemicalType.builder().id(UUID.randomUUID()).name("New Solvent").build();

        ChemicalTypeDto savedDto = new ChemicalTypeDto(savedEntity.getId(), "New Solvent", null);
        List<ChemicalType> allEntitiesAfterSave = List.of(mockEntity, savedEntity);
        List<ChemicalTypeDto> allDtosAfterSave = List.of(mockDto, savedDto);

        when(repository.existsByNameIgnoreCase("New Solvent")).thenReturn(false);
        when(mapper.toEntity(createDto)).thenReturn(entityToSave);
        when(repository.save(entityToSave)).thenReturn(savedEntity);

        when(repository.findAll()).thenReturn(allEntitiesAfterSave);
        when(mapper.toDtoList(allEntitiesAfterSave)).thenReturn(allDtosAfterSave);

        List<ChemicalTypeDto> result = service.createOptions(createDto);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Solvent", result.get(0).name());
        assertEquals("New Solvent", result.get(1).name());

        verify(repository, times(1)).existsByNameIgnoreCase("New Solvent");
        verify(repository, times(1)).save(entityToSave);
        verify(repository, times(1)).findAll();
    }

    @Test
    void createOptions_ShouldThrowResourceConflictException_WhenNameAlreadyExists() {
        ChemicalTypeDto conflictDto = new ChemicalTypeDto(null, "Solvent", null);

        when(repository.existsByNameIgnoreCase("Solvent")).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> service.createOptions(conflictDto));
        verify(repository, never()).save(any(ChemicalType.class));
        verify(repository, never()).findAll();
    }

    @Test
    void archiveOption_ShouldSetArchivedAt_WhenExists() {
        when(repository.findById(VALID_UUID)).thenReturn(Optional.of(mockEntity));

        service.archiveOption(VALID_UUID);

        assertNotNull(mockEntity.getArchivedAt());
        verify(repository, times(1)).save(mockEntity);
    }

    @Test
    void archiveOption_ShouldThrowResourceNotFound_WhenDoesNotExist() {
        when(repository.findById(VALID_UUID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.archiveOption(VALID_UUID));
        verify(repository, never()).save(any(ChemicalType.class));
    }

    @Test
    void restoreOption_ShouldClearArchivedAt_WhenExists() {
        mockEntity.setArchivedAt(Instant.now());
        when(repository.findById(VALID_UUID)).thenReturn(Optional.of(mockEntity));

        service.restoreOption(VALID_UUID);

        assertNull(mockEntity.getArchivedAt());
        verify(repository, times(1)).save(mockEntity);
    }

    @Test
    void restoreOption_ShouldThrowResourceNotFound_WhenDoesNotExist() {
        when(repository.findById(VALID_UUID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.restoreOption(VALID_UUID));
        verify(repository, never()).save(any(ChemicalType.class));
    }
}
