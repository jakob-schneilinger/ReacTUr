package com.reactur.backend.services;

import com.reactur.backend.client.JobStatusResult;
import com.reactur.backend.client.StructureApiClient;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.enums.JobStatus;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exeptions.ResourceConflictException;
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
    void uploadStructure_shouldSaveAndReturnDto() {
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
        assertThat(savedEntity.getFlag()).isEqualTo(StructureFlag.ACTIVE);

        assertThat(result.name()).isEqualTo("test.xyz");
        assertThat(result.flag()).isEqualTo(StructureFlag.ACTIVE);
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
    void uploadStructure_shouldDeleteExistingAndSaveNewAsActive() {
        MockMultipartFile file = new MockMultipartFile("file", "test.xyz", "text/plain", "XYZ".getBytes());
        ReactantStructure oldStructure = ReactantStructure.builder().flag(StructureFlag.REJECTED).build();

        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(mockReactant));
        when(structureRepository.findByReactant_Id(REACTANT_ID)).thenReturn(List.of(oldStructure));
        when(structureRepository.save(any(ReactantStructure.class))).thenAnswer(i -> i.getArgument(0));

        StructureDto result = structureService.uploadStructure(REACTANT_ID, file);

        verify(structureRepository, times(1)).deleteAll(List.of(oldStructure));
        verify(structureRepository).save(structureCaptor.capture());
        assertThat(structureCaptor.getValue().getFlag()).isEqualTo(StructureFlag.ACTIVE);
        assertThat(result.flag()).isEqualTo(StructureFlag.ACTIVE);
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
    void generateStructureFromAPI_shouldTriggerApi_whenOnlyActiveExistsFromManualUpload() {
        ReactantStructure active = ReactantStructure.builder().flag(StructureFlag.ACTIVE).build();
        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(mockReactant));
        when(structureRepository.findByReactant_Id(REACTANT_ID)).thenReturn(List.of(active));
        when(structureApiClient.triggerJob(anyString())).thenReturn("job_123");

        structureService.generateStructureFromAPI(REACTANT_ID);

        verify(structureApiClient, times(1)).triggerJob("CCO");
        verify(structureRepository, times(1)).save(any(ReactantStructure.class));
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
    void generateStructureFromAPI_shouldThrowConflict_whenPendingExists() {
        ReactantStructure pending = ReactantStructure.builder().flag(StructureFlag.PENDING).build();
        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(mockReactant));
        when(structureRepository.findByReactant_Id(REACTANT_ID)).thenReturn(List.of(pending));

        assertThrows(ResourceConflictException.class, () -> structureService.generateStructureFromAPI(REACTANT_ID));
        verify(structureApiClient, never()).triggerJob(any());
    }

    @Test
    void generateStructureFromAPI_shouldThrowConflict_whenReadyExists() {
        ReactantStructure ready = ReactantStructure.builder().flag(StructureFlag.READY).build();
        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(mockReactant));
        when(structureRepository.findByReactant_Id(REACTANT_ID)).thenReturn(List.of(ready));

        assertThrows(ResourceConflictException.class, () -> structureService.generateStructureFromAPI(REACTANT_ID));
    }

    @Test
    void generateStructureFromAPI_shouldThrowConflict_whenActiveAndRejectedExist() {
        ReactantStructure active = ReactantStructure.builder().flag(StructureFlag.ACTIVE).build();
        ReactantStructure rejected = ReactantStructure.builder().flag(StructureFlag.REJECTED).build();
        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(mockReactant));
        when(structureRepository.findByReactant_Id(REACTANT_ID)).thenReturn(List.of(active, rejected));

        assertThrows(ResourceConflictException.class, () -> structureService.generateStructureFromAPI(REACTANT_ID));
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

        when(structureRepository.saveAll(anyIterable())).thenAnswer(i -> i.getArgument(0));

        StructureDto result = structureService.selectStructure(STRUCTURE_ID);

        assertThat(mockPendingStructure.getFlag()).isEqualTo(StructureFlag.ACTIVE);
        assertThat(otherStructure.getFlag()).isEqualTo(StructureFlag.REJECTED);
        verify(structureRepository, times(1)).saveAll(anyIterable());
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
    void selectStructure_shouldSwapActiveAndRejected() {
        ReactantStructure targetToActivate = ReactantStructure.builder().id(STRUCTURE_ID).reactant(mockReactant).flag(StructureFlag.REJECTED).build();
        ReactantStructure currentlyActive = ReactantStructure.builder().id(UUID.randomUUID()).reactant(mockReactant).flag(StructureFlag.ACTIVE).build();

        when(structureRepository.findById(STRUCTURE_ID)).thenReturn(Optional.of(targetToActivate));
        when(structureRepository.findByReactant_Id(REACTANT_ID)).thenReturn(List.of(targetToActivate, currentlyActive));

        StructureDto result = structureService.selectStructure(STRUCTURE_ID);

        assertThat(targetToActivate.getFlag()).isEqualTo(StructureFlag.ACTIVE);
        assertThat(currentlyActive.getFlag()).isEqualTo(StructureFlag.REJECTED);

        verify(structureRepository, times(1)).saveAll(any());
        assertThat(result.flag()).isEqualTo(StructureFlag.ACTIVE);
    }

    @Test
    void getStructuresByReactant_shouldReturnMappedDtos() {

        when(reactantRepository.findById(REACTANT_ID)).thenReturn(Optional.of(new Reactant()));
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

    @Test
    void fetchPendingStructuresFromAPI_whenNoPendingStructures_shouldReturnImmediately() {
        when(structureRepository.findByFlag(StructureFlag.PENDING)).thenReturn(List.of());

        structureService.fetchPendingStructuresFromAPI();

        verifyNoInteractions(structureApiClient);
        verify(structureRepository, never()).save(any());
    }

    @Test
    void fetchPendingStructuresFromAPI_whenJobFinishedWithMultipleStructures_shouldUpdateExistingAndCreateAdditional() {
        when(structureRepository.findByFlag(StructureFlag.PENDING)).thenReturn(List.of(mockPendingStructure));
        JobStatusResult result = new JobStatusResult(JobStatus.SUCCEEDED, List.of("struct_A", "struct_B", "struct_C"), null);
        when(structureApiClient.checkJobStatus("job_123")).thenReturn(result);
        when(structureRepository.save(any(ReactantStructure.class))).thenAnswer(i -> i.getArgument(0));

        structureService.fetchPendingStructuresFromAPI();

        verify(structureRepository, times(3)).save(structureCaptor.capture());
        List<ReactantStructure> savedEntities = structureCaptor.getAllValues();

        ReactantStructure firstSaved = savedEntities.get(0);
        assertThat(firstSaved).isSameAs(mockPendingStructure);
        assertThat(firstSaved.getStructure()).isEqualTo("struct_A");
        assertThat(firstSaved.getFlag()).isEqualTo(StructureFlag.READY);

        ReactantStructure secondSaved = savedEntities.get(1);
        assertThat(secondSaved).isNotSameAs(mockPendingStructure);
        assertThat(secondSaved.getReactant()).isEqualTo(mockReactant);
        assertThat(secondSaved.getExternalJobId()).isEqualTo("job_123");
        assertThat(secondSaved.getStructure()).isEqualTo("struct_B");
        assertThat(secondSaved.getFlag()).isEqualTo(StructureFlag.READY);

        ReactantStructure thirdSaved = savedEntities.get(2);
        assertThat(thirdSaved).isNotSameAs(mockPendingStructure);
        assertThat(thirdSaved.getReactant()).isEqualTo(mockReactant);
        assertThat(thirdSaved.getExternalJobId()).isEqualTo("job_123");
        assertThat(thirdSaved.getStructure()).isEqualTo("struct_C");
        assertThat(thirdSaved.getFlag()).isEqualTo(StructureFlag.READY);
    }

    @Test
    void fetchPendingStructuresFromAPI_whenPrimaryStructureIsBlank_shouldMarkFailedInsteadOfPersistingBlankContent() {
        when(structureRepository.findByFlag(StructureFlag.PENDING)).thenReturn(List.of(mockPendingStructure));
        JobStatusResult result = new JobStatusResult(JobStatus.SUCCEEDED, List.of("   "), null);
        when(structureApiClient.checkJobStatus("job_123")).thenReturn(result);

        structureService.fetchPendingStructuresFromAPI();

        verify(structureRepository, times(1)).save(structureCaptor.capture());
        ReactantStructure savedEntity = structureCaptor.getValue();
        assertThat(savedEntity).isSameAs(mockPendingStructure);
        assertThat(savedEntity.getFlag()).isEqualTo(StructureFlag.FAILED);
        assertThat(savedEntity.getStructure()).isNull();
    }

    @Test
    void fetchPendingStructuresFromAPI_whenPrimaryStructureExceedsMaxLength_shouldMarkFailed() {
        when(structureRepository.findByFlag(StructureFlag.PENDING)).thenReturn(List.of(mockPendingStructure));
        String oversizedStructure = "A".repeat(1_000_001);
        JobStatusResult result = new JobStatusResult(JobStatus.SUCCEEDED, List.of(oversizedStructure), null);
        when(structureApiClient.checkJobStatus("job_123")).thenReturn(result);

        structureService.fetchPendingStructuresFromAPI();

        verify(structureRepository, times(1)).save(structureCaptor.capture());
        ReactantStructure savedEntity = structureCaptor.getValue();
        assertThat(savedEntity).isSameAs(mockPendingStructure);
        assertThat(savedEntity.getFlag()).isEqualTo(StructureFlag.FAILED);
        assertThat(savedEntity.getStructure()).isNull();
    }

    @Test
    void fetchPendingStructuresFromAPI_whenAnAdditionalStructureIsBlank_shouldSkipItButKeepValidOnes() {
        when(structureRepository.findByFlag(StructureFlag.PENDING)).thenReturn(List.of(mockPendingStructure));
        JobStatusResult result = new JobStatusResult(JobStatus.SUCCEEDED, List.of("struct_A", "   ", "struct_C"), null);
        when(structureApiClient.checkJobStatus("job_123")).thenReturn(result);
        when(structureRepository.save(any(ReactantStructure.class))).thenAnswer(i -> i.getArgument(0));

        structureService.fetchPendingStructuresFromAPI();

        // Only the valid primary structure and the one valid additional structure are saved;
        // the blank one in the middle is skipped entirely.
        verify(structureRepository, times(2)).save(structureCaptor.capture());
        List<ReactantStructure> savedEntities = structureCaptor.getAllValues();

        ReactantStructure firstSaved = savedEntities.get(0);
        assertThat(firstSaved).isSameAs(mockPendingStructure);
        assertThat(firstSaved.getStructure()).isEqualTo("struct_A");
        assertThat(firstSaved.getFlag()).isEqualTo(StructureFlag.READY);

        ReactantStructure secondSaved = savedEntities.get(1);
        assertThat(secondSaved).isNotSameAs(mockPendingStructure);
        assertThat(secondSaved.getStructure()).isEqualTo("struct_C");
        assertThat(secondSaved.getFlag()).isEqualTo(StructureFlag.READY);
    }

    @Test
    void fetchPendingStructuresFromAPI_whenJobFinishedWithEmptyStructuresList_shouldMarkFailed() {
        when(structureRepository.findByFlag(StructureFlag.PENDING)).thenReturn(List.of(mockPendingStructure));
        JobStatusResult result = new JobStatusResult(JobStatus.SUCCEEDED, List.of(), null);
        when(structureApiClient.checkJobStatus("job_123")).thenReturn(result);

        structureService.fetchPendingStructuresFromAPI();

        verify(structureRepository, times(1)).save(structureCaptor.capture());
        ReactantStructure savedEntity = structureCaptor.getValue();
        assertThat(savedEntity).isSameAs(mockPendingStructure);
        assertThat(savedEntity.getFlag()).isEqualTo(StructureFlag.FAILED);
    }

    @Test
    void fetchPendingStructuresFromAPI_whenJobFinishedWithNullStructuresList_shouldMarkFailed() {
        when(structureRepository.findByFlag(StructureFlag.PENDING)).thenReturn(List.of(mockPendingStructure));
        JobStatusResult result = new JobStatusResult(JobStatus.SUCCEEDED, null, null);
        when(structureApiClient.checkJobStatus("job_123")).thenReturn(result);

        structureService.fetchPendingStructuresFromAPI();

        verify(structureRepository, times(1)).save(structureCaptor.capture());
        ReactantStructure savedEntity = structureCaptor.getValue();
        assertThat(savedEntity).isSameAs(mockPendingStructure);
        assertThat(savedEntity.getFlag()).isEqualTo(StructureFlag.FAILED);
    }

    @Test
    void fetchPendingStructuresFromAPI_whenJobFailed_shouldMarkFailed() {
        when(structureRepository.findByFlag(StructureFlag.PENDING)).thenReturn(List.of(mockPendingStructure));
        JobStatusResult result = new JobStatusResult(JobStatus.FAILED, null, "External service error");
        when(structureApiClient.checkJobStatus("job_123")).thenReturn(result);

        structureService.fetchPendingStructuresFromAPI();

        verify(structureRepository, times(1)).save(structureCaptor.capture());
        ReactantStructure savedEntity = structureCaptor.getValue();
        assertThat(savedEntity).isSameAs(mockPendingStructure);
        assertThat(savedEntity.getFlag()).isEqualTo(StructureFlag.FAILED);
    }

    @Test
    void fetchPendingStructuresFromAPI_whenJobStillRunning_shouldNotSave() {
        when(structureRepository.findByFlag(StructureFlag.PENDING)).thenReturn(List.of(mockPendingStructure));
        JobStatusResult result = new JobStatusResult(JobStatus.RUNNING, null, null);
        when(structureApiClient.checkJobStatus("job_123")).thenReturn(result);

        structureService.fetchPendingStructuresFromAPI();

        verify(structureRepository, never()).save(any());
    }

    @Test
    void fetchPendingStructuresFromAPI_whenOneJobThrows_shouldStillProcessRemainingJobs() {
        ReactantStructure secondPending = ReactantStructure.builder()
                .id(UUID.randomUUID())
                .reactant(mockReactant)
                .flag(StructureFlag.PENDING)
                .externalJobId("job_456")
                .build();

        when(structureRepository.findByFlag(StructureFlag.PENDING))
                .thenReturn(List.of(mockPendingStructure, secondPending));
        when(structureApiClient.checkJobStatus("job_123")).thenThrow(new RuntimeException("connection refused"));
        JobStatusResult secondResult = new JobStatusResult(JobStatus.FAILED, null, "External service error");
        when(structureApiClient.checkJobStatus("job_456")).thenReturn(secondResult);

        structureService.fetchPendingStructuresFromAPI();

        verify(structureRepository, times(1)).save(structureCaptor.capture());
        ReactantStructure savedEntity = structureCaptor.getValue();
        assertThat(savedEntity).isSameAs(secondPending);
        assertThat(savedEntity.getFlag()).isEqualTo(StructureFlag.FAILED);
    }
}
