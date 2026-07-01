package com.reactur.backend;

import com.reactur.backend.client.StructureApiClient;
import com.reactur.backend.dtos.ReactantDtos.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.StructureMapper;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.services.implementations.StructureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StructureServiceTest {

    @Mock
    private ReactantStructureRepository structureRepository;

    @Mock
    private ReactantRepository reactantRepository;

    @Mock
    private StructureApiClient structureApiClient;

    @Spy
    private StructureMapper structureMapper = Mappers.getMapper(StructureMapper.class);

    @InjectMocks
    private StructureServiceImpl structureService;

    @Captor
    private ArgumentCaptor<ReactantStructure> structureCaptor;

    private Reactant mockReactant;
    private ReactantStructure mockPendingStructure;
    private final UUID REACTANT_ID = UUID.randomUUID();
    private final UUID STRUCTURE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockReactant = Reactant.builder()
                .id(REACTANT_ID)
                .smiles("CCO")
                .build();

        mockPendingStructure = ReactantStructure.builder()
                .id(STRUCTURE_ID)
                .reactant(mockReactant)
                .flag(StructureFlag.PENDING)
                .externalJobId("job_123")
                .build();
    }

    @Test
    void uploadStructure_shouldSaveAndReturnDto() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xyz", "text/plain", "XYZ content".getBytes()
        );

        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(mockReactant));
        when(structureRepository.save(any(ReactantStructure.class))).thenAnswer(i -> {
            ReactantStructure saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        StructureDto result = structureService.uploadStructure(REACTANT_ID, file);

        verify(structureRepository).save(structureCaptor.capture());
        ReactantStructure savedEntity = structureCaptor.getValue();

        assertThat(savedEntity.getReactant()).isEqualTo(mockReactant);
        assertThat(savedEntity.getFileName()).isEqualTo("test.xyz");
        assertThat(savedEntity.getStructure()).isEqualTo("XYZ content");
        assertThat(savedEntity.getFlag()).isEqualTo(StructureFlag.READY);

        assertThat(result.name()).isEqualTo("test.xyz");
        assertThat(result.flag()).isEqualTo(StructureFlag.READY);
    }

    @Test
    void uploadStructure_whenReactantNotFound_shouldThrowResourceNotFoundException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.xyz", "text/plain", "XYZ".getBytes());

        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> structureService.uploadStructure(REACTANT_ID, file)
        );

        assertThat(exception.getMessage()).containsIgnoringCase("Reactant");
        verify(structureRepository, never()).save(any());
    }

    @Test
    void uploadStructure_whenFileIsEmpty_shouldThrowIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.xyz", "text/plain", new byte[0]);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> structureService.uploadStructure(REACTANT_ID, emptyFile)
        );

        assertThat(exception.getMessage()).containsIgnoringCase("empty");
        verify(reactantRepository, never()).findById(any());
        verify(structureRepository, never()).save(any());
    }

    @Test
    void generateStructureFromAPI_shouldTriggerApiAndSavePendingEntity() {
        String mockJobId = "24b31e13f36f4fd0808a5cd2f0abb781";
        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(mockReactant));
        when(structureApiClient.triggerJob(mockReactant.getSmiles())).thenReturn(mockJobId);

        structureService.generateStructureFromAPI(REACTANT_ID);

        verify(structureRepository).save(structureCaptor.capture());
        ReactantStructure savedEntity = structureCaptor.getValue();

        assertThat(savedEntity.getReactant()).isEqualTo(mockReactant);
        assertThat(savedEntity.getExternalJobId()).isEqualTo(mockJobId);
        assertThat(savedEntity.getFlag()).isEqualTo(StructureFlag.PENDING);
    }

    @Test
    void generateStructureFromAPI_whenReactantNotFound_shouldThrowResourceNotFoundException() {
        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> structureService.generateStructureFromAPI(REACTANT_ID)
        );

        verify(structureApiClient, never()).triggerJob(anyString());
        verify(structureRepository, never()).save(any());
    }

    @Test
    void generateStructureFromAPI_whenApiFails_shouldBubbleUpException() {
        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(mockReactant));

        RuntimeException apiError = new RuntimeException("API is down");
        when(structureApiClient.triggerJob(anyString())).thenThrow(apiError);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> structureService.generateStructureFromAPI(REACTANT_ID)
        );

        assertThat(exception.getMessage()).isEqualTo("API is down");
        verify(structureRepository, never()).save(any());
    }

    @Test
    void selectStructure_shouldSetActiveAndRejectOthers() {
        ReactantStructure otherStructure = ReactantStructure.builder()
                .id(UUID.randomUUID())
                .reactant(mockReactant)
                .flag(StructureFlag.READY)
                .build();

        when(structureRepository.findById(STRUCTURE_ID)).thenReturn(Optional.of(mockPendingStructure));
        when(structureRepository.findByReactant_Id(REACTANT_ID)).thenReturn(List.of(mockPendingStructure, otherStructure));
        when(structureRepository.save(any(ReactantStructure.class))).thenAnswer(i -> i.getArgument(0));

        StructureDto result = structureService.selectStructure(STRUCTURE_ID);

        assertThat(mockPendingStructure.getFlag()).isEqualTo(StructureFlag.ACTIVE);

        assertThat(otherStructure.getFlag()).isEqualTo(StructureFlag.REJECTED);

        verify(structureRepository, times(2)).save(any(ReactantStructure.class));

        assertThat(result.flag()).isEqualTo(StructureFlag.ACTIVE);
        assertThat(result.id()).isEqualTo(STRUCTURE_ID);
    }

    @Test
    void selectStructure_whenStructureNotFound_shouldThrowResourceNotFoundException() {
        when(structureRepository.findById(STRUCTURE_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> structureService.selectStructure(STRUCTURE_ID)
        );

        verify(structureRepository, never()).findByReactant_Id(any());
        verify(structureRepository, never()).save(any());
    }

    @Test
    void selectStructure_whenStructureIsAlreadyActive_shouldDoNothingAndReturnDto() {
        mockPendingStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findById(STRUCTURE_ID)).thenReturn(Optional.of(mockPendingStructure));

        StructureDto result = structureService.selectStructure(STRUCTURE_ID);

        verify(structureRepository, never()).findByReactant_Id(any());
        verify(structureRepository, never()).save(any());

        assertThat(result.flag()).isEqualTo(StructureFlag.ACTIVE);
    }

    @Test
    void getStructuresByReactant_shouldReturnMappedDtos() {
        when(structureRepository.findByReactant_Id(REACTANT_ID)).thenReturn(List.of(mockPendingStructure));

        List<StructureDto> result = structureService.getStructuresByReactant(REACTANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(STRUCTURE_ID);
        assertThat(result.getFirst().flag()).isEqualTo(StructureFlag.PENDING);
    }

    @Test
    void getStructuresByReactant_whenReactantNotFound_shouldThrowResourceNotFoundException() {
        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> structureService.getStructuresByReactant(REACTANT_ID)
        );

        verify(structureRepository, never()).findByReactant_Id(any());
    }

    @Test
    void getStructuresByReactant_whenReactantExistsButNoStructures_shouldReturnEmptyList() {
        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(mockReactant));
        when(structureRepository.findByReactant_Id(REACTANT_ID)).thenReturn(List.of());

        List<StructureDto> result = structureService.getStructuresByReactant(REACTANT_ID);

        assertThat(result).isEmpty();
        verify(structureRepository, times(1)).findByReactant_Id(REACTANT_ID);
    }
}
