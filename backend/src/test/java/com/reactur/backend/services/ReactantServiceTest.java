package com.reactur.backend.services;

import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.ChemicalTypeRepository;
import com.reactur.backend.validators.ReactantValidator;
import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.UpdateReactantDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.exeptions.ValidationException;
import com.reactur.backend.mappers.ReactantMapper;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.services.implementations.ReactantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactantServiceTest {

    @Mock
    private ReactantRepository reactantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StructureService structureService;

    @Mock
    private ChemicalTypeRepository chemicalTypeRepository;

    @Spy
    private ReactantMapper reactantMapper = Mappers.getMapper(ReactantMapper.class);

    @InjectMocks
    private ReactantServiceImpl reactantService;

    private CreateReactantDto sampleCreateDto;
    private UpdateReactantDto sampleUpdateDto;
    private User mockUser;
    private Reactant mockReactant;
    private final UUID VALID_UUID = UUID.randomUUID();
    private final String MOCK_USER_ID = "keycloak-123";
    private ChemicalType mockChemicalType;
    private final UUID VALID_CHEMICAL_TYPE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReactantValidator validator = new ReactantValidator(reactantRepository);
        ReflectionTestUtils.setField(reactantService, "reactantValidator", validator);

        mockUser = new User();
        mockUser.setId(MOCK_USER_ID);

        mockChemicalType = new ChemicalType();
        mockChemicalType.setId(VALID_CHEMICAL_TYPE_ID);
        mockChemicalType.setName("Solvent");

        sampleCreateDto = new CreateReactantDto(
                "CCO", VALID_CHEMICAL_TYPE_ID, "Ethanol",
                List.of("EtOH", "Alkohol"), "Ethyl alcohol", "64-17-5",
                "LFQSCWFLJHTTHZ-UHFFFAOYSA-N", "A test comment", List.of("Ref1", "Ref2")
        );

        sampleUpdateDto = new UpdateReactantDto(
                "CCO", VALID_CHEMICAL_TYPE_ID, "Ethanol",
                List.of("EtOH", "Alkohol"), "Ethyl alcohol", "64-17-5",
                "LFQSCWFLJHTTHZ-UHFFFAOYSA-N", "A test comment", List.of("Ref1", "Ref2")
        );

        mockReactant = Reactant.builder()
                .id(VALID_UUID)
                .type(mockChemicalType)
                .shortName("Ethanol")
                .smiles("CCO")
                .chemicalName("Ethyl alcohol")
                .casNumber("64-17-5")
                .inChiKey("LFQSCWFLJHTTHZ-UHFFFAOYSA-N")
                .comment("A test comment")
                .additionalShortNames(List.of("EtOH", "Alkohol"))
                .references(List.of("Ref1", "Ref2"))
                .createdBy(mockUser)
                .build();
    }
    @Test
    void createReactant_ShouldSaveSuccessfully_WhenValidData() {
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);
        when(reactantRepository.save(any(Reactant.class))).thenAnswer(invocation -> {
            Reactant saved = invocation.getArgument(0);
            saved.setId(VALID_UUID);
            return saved;
        });
        when(chemicalTypeRepository.findById(VALID_CHEMICAL_TYPE_ID)).thenReturn(Optional.of(mockChemicalType));

        ReactantDetailDto result = reactantService.createReactant(sampleCreateDto, MOCK_USER_ID);

        assertNotNull(result);
        assertEquals(VALID_UUID, result.uuid());
        assertEquals("Ethanol", result.shortName());
        assertEquals("A test comment", result.comment());
        assertIterableEquals(List.of("EtOH", "Alkohol"), result.additionalShortNames());
        assertTrue(result.structures().isEmpty());
        verify(reactantRepository, times(1)).save(any(Reactant.class));
    }

    @Test
    void createReactant_ShouldThrowResourceNotFound_WhenChemicalTypeDoesNotExist() {
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);

        when(chemicalTypeRepository.findById(VALID_CHEMICAL_TYPE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reactantService.createReactant(sampleCreateDto, MOCK_USER_ID));
        verify(reactantRepository, never()).save(any(Reactant.class));
    }

    @Test
    void createReactant_ShouldThrowResourceNotFound_WhenUserDoesNotExist() {
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reactantService.createReactant(sampleCreateDto, MOCK_USER_ID));
        verify(reactantRepository, never()).save(any(Reactant.class));
    }

    @Test
    void createReactant_ShouldThrowResourceConflictException_WhenShortNameExists() {
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> reactantService.createReactant(sampleCreateDto, MOCK_USER_ID));
        verify(reactantRepository, never()).save(any(Reactant.class));
    }

    @Test
    void createReactant_ShouldThrowConflict_WhenAdditionalShortNameAlreadyExistsAsPrimary() {
        CreateReactantDto newReactantDto = new CreateReactantDto(
                "C", UUID.randomUUID(), "Methane",
                List.of("Ethanol"),
                "Methane Gas", null, null, null, null
        );

        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> reactantService.createReactant(newReactantDto, MOCK_USER_ID));
    }

    @Test
    void createReactant_ShouldThrowValidationException_WhenAdditionalShortNamesDifferOnlyByCase() {
        CreateReactantDto newReactantDto = new CreateReactantDto(
                "Cl", VALID_CHEMICAL_TYPE_ID, "Hydrochloric Acid",
                List.of("HCl", "hcl"),
                "Hydrochloric acid", null, null, null, null
        );

        assertThrows(ValidationException.class, () -> reactantService.createReactant(newReactantDto, MOCK_USER_ID));
        verify(reactantRepository, never()).hasNameConflict(anyCollection());
        verify(reactantRepository, never()).save(any(Reactant.class));
    }

    @Test
    void createReactant_ShouldThrowValidationException_WhenShortNameAndAdditionalShortNameDifferOnlyByCase() {
        CreateReactantDto newReactantDto = new CreateReactantDto(
                "Cl", VALID_CHEMICAL_TYPE_ID, "HCl",
                List.of("hcl"),
                "Hydrochloric acid", null, null, null, null
        );

        assertThrows(ValidationException.class, () -> reactantService.createReactant(newReactantDto, MOCK_USER_ID));
        verify(reactantRepository, never()).hasNameConflict(anyCollection());
        verify(reactantRepository, never()).save(any(Reactant.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createReactant_ShouldPassLowercasedNamesToRepository_WhenCheckingForConflicts() {
        CreateReactantDto newReactantDto = new CreateReactantDto(
                "Cl", VALID_CHEMICAL_TYPE_ID, "HCl",
                List.of("Muriatic Acid"),
                "Hydrochloric acid", null, null, null, null
        );

        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);
        when(reactantRepository.save(any(Reactant.class))).thenAnswer(invocation -> {
            Reactant saved = invocation.getArgument(0);
            saved.setId(VALID_UUID);
            return saved;
        });
        when(chemicalTypeRepository.findById(VALID_CHEMICAL_TYPE_ID)).thenReturn(Optional.of(mockChemicalType));

        reactantService.createReactant(newReactantDto, MOCK_USER_ID);

        ArgumentCaptor<Collection<String>> namesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(reactantRepository).hasNameConflict(namesCaptor.capture());
        Collection<String> capturedNames = namesCaptor.getValue();

        assertEquals(2, capturedNames.size());
        assertTrue(capturedNames.contains("hcl"));
        assertTrue(capturedNames.contains("muriatic acid"));
    }

    @Test
    void updateReactant_ShouldUpdateSuccessfully_WhenValidData() {
        UpdateReactantDto updateDto = new UpdateReactantDto(
                "C", null, "Methane", List.of(), "Methane Gas", "74-82-8",
                "VNWKTOKETHGBQD-UHFFFAOYSA-N", "Updated Comment", List.of()
        );

        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.of(mockReactant));
        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(VALID_UUID))).thenReturn(false);
        when(reactantRepository.save(any(Reactant.class))).thenReturn(mockReactant);

        StructureDto mockStruct = new StructureDto(UUID.randomUUID(), VALID_UUID, "test.xyz", "data", StructureFlag.ACTIVE);
        when(structureService.getStructuresByReactant(VALID_UUID)).thenReturn(List.of(mockStruct));

        ReactantDetailDto result = reactantService.updateReactant(VALID_UUID, updateDto);

        assertNotNull(result);
        assertEquals(1, result.structures().size());
        assertEquals(StructureFlag.ACTIVE, result.structures().getFirst().flag());
        verify(reactantRepository, times(1)).save(mockReactant);
    }

    @Test
    void updateReactant_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reactantService.updateReactant(VALID_UUID, sampleUpdateDto));
        verify(reactantRepository, never()).save(any(Reactant.class));
    }

    @Test
    void updateReactant_ShouldThrowResourceConflict_WhenNewShortNameBelongsToOtherReactant() {
        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.of(mockReactant));
        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(VALID_UUID))).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> reactantService.updateReactant(VALID_UUID, sampleUpdateDto));
        verify(reactantRepository, never()).save(any(Reactant.class));
    }

    @Test
    void updateReactant_ShouldOnlyUpdateProvidedFields_WhenPatching() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, null, null, "Neuer Chemischer Name", null, null, null, null
        );

        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.of(mockReactant));
        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(VALID_UUID))).thenReturn(false);
        when(reactantRepository.save(any(Reactant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(structureService.getStructuresByReactant(VALID_UUID)).thenReturn(List.of());

        ReactantDetailDto result = reactantService.updateReactant(VALID_UUID, patchDto);

        assertEquals("Neuer Chemischer Name", result.chemicalName());
        assertEquals("Ethanol", result.shortName());
        assertEquals("CCO", result.smiles());
    }

    @Test
    void updateReactant_ShouldClearFields_WhenEmptyStringOrListProvided() {
        UpdateReactantDto clearDto = new UpdateReactantDto(
                null, null, null, Collections.emptyList(), null, "", "", "", Collections.emptyList()
        );

        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.of(mockReactant));
        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(VALID_UUID))).thenReturn(false);
        when(reactantRepository.save(any(Reactant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(structureService.getStructuresByReactant(VALID_UUID)).thenReturn(List.of());

        ReactantDetailDto result = reactantService.updateReactant(VALID_UUID, clearDto);

        assertEquals("", result.casNumber());
        assertEquals("", result.inChiKey());
        assertTrue(result.additionalShortNames().isEmpty());
    }

    @Test
    void getReactantDetail_ShouldReturnDetails_WhenExists() {
        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.of(mockReactant));
        when(structureService.getStructuresByReactant(VALID_UUID)).thenReturn(List.of());

        ReactantDetailDto result = reactantService.getReactantDetail(VALID_UUID);

        assertNotNull(result);
        assertEquals(VALID_UUID, result.uuid());
        assertEquals("Ethanol", result.shortName());
        assertIterableEquals(List.of("Ref1", "Ref2"), result.references());
        assertTrue(result.structures().isEmpty());
    }

    @Test
    void getReactantDetail_ShouldThrowResourceNotFound_WhenDoesNotExist() {
        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reactantService.getReactantDetail(VALID_UUID));
    }

    @Test
    void getAllReactants_ShouldReturnOnlyActiveReactants() {
        when(reactantRepository.findAllByArchivedAtIsNull()).thenReturn(List.of(mockReactant));

        StructureDto pendingStruct = new StructureDto(UUID.randomUUID(), VALID_UUID, "test", "data", StructureFlag.PENDING);
        when(structureService.getStructuresByReactant(VALID_UUID)).thenReturn(List.of(pendingStruct));

        List<ReactantDto> result = reactantService.getAllReactants();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Ethanol", result.getFirst().shortName());
        assertEquals(StructureFlag.PENDING, result.getFirst().flag());
    }

    @Test
    void getArchivedReactants_ShouldReturnOnlyArchivedReactants() {
        Reactant archivedReactant = Reactant.builder()
                .id(UUID.randomUUID())
                .shortName("Archived Chem")
                .smiles("C")
                .archivedAt(Instant.now())
                .build();

        when(reactantRepository.findAllByArchivedAtIsNotNull()).thenReturn(List.of(archivedReactant));
        when(structureService.getStructuresByReactant(any())).thenReturn(List.of());

        List<ReactantDto> result = reactantService.getArchivedReactants();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Archived Chem", result.getFirst().shortName());
        assertNull(result.getFirst().flag());
    }

    @Test
    void archiveReactant_ShouldSetArchivedAt_WhenExists() {
        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.of(mockReactant));

        reactantService.archiveReactant(VALID_UUID);

        assertNotNull(mockReactant.getArchivedAt());
        verify(reactantRepository, times(1)).save(mockReactant);
    }

    @Test
    void archiveReactant_ShouldThrowResourceNotFound_WhenDoesNotExist() {
        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reactantService.archiveReactant(VALID_UUID));
        verify(reactantRepository, never()).save(any(Reactant.class));
    }

    @Test
    void restoreReactant_ShouldClearArchivedAt_WhenExists() {
        mockReactant.setArchivedAt(Instant.now());
        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.of(mockReactant));
        when(reactantRepository.save(any(Reactant.class))).thenReturn(mockReactant);
        when(structureService.getStructuresByReactant(VALID_UUID)).thenReturn(List.of());

        ReactantDto result = reactantService.restoreReactant(VALID_UUID);

        assertNull(mockReactant.getArchivedAt());
        assertNotNull(result);
        verify(reactantRepository, times(1)).save(mockReactant);
    }

    @Test
    void restoreReactant_ShouldThrowResourceNotFound_WhenDoesNotExist() {
        when(reactantRepository.findById(VALID_UUID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reactantService.restoreReactant(VALID_UUID));
        verify(reactantRepository, never()).save(any(Reactant.class));
    }
}