package com.reactur.backend.validators;

import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.UpdateReactantDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.exceptions.ResourceConflictException;
import com.reactur.backend.exceptions.UnprocessableEntityException;
import com.reactur.backend.exceptions.ValidationException;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.ChemicalTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReactantValidator}, covering the short-name/CAS/InChIKey conflict
 * validation (pre-existing behavior, previously only covered indirectly through
 * {@code ReactantServiceTest}) as well as the {@code User}/{@code ChemicalType} resolution that
 * this validator now also performs and returns via {@link ReactantValidator.ValidatedReferences}.
 */
@ExtendWith(MockitoExtension.class)
class ReactantValidatorTest {

    @Mock private ReactantRepository reactantRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChemicalTypeRepository chemicalTypeRepository;

    @InjectMocks
    private ReactantValidator reactantValidator;

    private final String userId = "keycloak-123";
    private final UUID chemicalTypeId = UUID.randomUUID();
    private final UUID reactantId = UUID.randomUUID();

    private User activeUser;
    private ChemicalType chemicalType;
    private Reactant existingReactant;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(userId);

        chemicalType = new ChemicalType();
        chemicalType.setId(chemicalTypeId);

        existingReactant = Reactant.builder()
                .id(reactantId)
                .shortName("Ethanol")
                .additionalShortNames(List.of("EtOH"))
                .casNumber("64-17-5")
                .inChiKey("LFQSCWFLJHTTHZ-UHFFFAOYSA-N")
                .type(chemicalType)
                .build();
    }

    private CreateReactantDto buildCreateDto(String shortName, List<String> additionalShortNames, String casNumber) {
        return new CreateReactantDto(
                "CCO", chemicalTypeId, shortName, additionalShortNames,
                "Ethyl alcohol", casNumber, null, null, null
        );
    }

    private CreateReactantDto buildCreateDtoWithInChiKey(String shortName, List<String> additionalShortNames, String inChiKey) {
        return new CreateReactantDto(
                "CCO", chemicalTypeId, shortName, additionalShortNames,
                "Ethyl alcohol", null, inChiKey, null, null
        );
    }

    // --- validateForCreate: happy path -----------------------------------

    @Test
    void validateForCreate_shouldReturnResolvedReferences_whenAllReferencesValid() {
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(chemicalTypeRepository.findById(chemicalTypeId)).thenReturn(Optional.of(chemicalType));

        ReactantValidator.ValidatedReferences result =
                reactantValidator.validateForCreate(buildCreateDto("Ethanol", List.of("EtOH"), null), userId);

        assertNotNull(result.activeUser());
        assertNotNull(result.chemicalType());
        assertEquals(activeUser, result.activeUser());
        assertEquals(chemicalType, result.chemicalType());
    }

    // --- validateForCreate: user / chemical type existence ---------------

    @Test
    void validateForCreate_shouldThrowIllegalState_whenCurrentUserDoesNotExist() {
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> reactantValidator.validateForCreate(buildCreateDto("Ethanol", List.of("EtOH"), null), userId));
    }

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenChemicalTypeDoesNotExist() {
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(chemicalTypeRepository.findById(chemicalTypeId)).thenReturn(Optional.empty());

        assertThrows(UnprocessableEntityException.class,
                () -> reactantValidator.validateForCreate(buildCreateDto("Ethanol", List.of("EtOH"), null), userId));
    }

    // --- validateForCreate: pre-existing conflict/validation paths -------

    @Test
    void validateForCreate_shouldThrowResourceConflict_whenShortNameAlreadyExists() {
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(true);

        assertThrows(ResourceConflictException.class,
                () -> reactantValidator.validateForCreate(buildCreateDto("Ethanol", List.of("EtOH"), null), userId));
    }

    @Test
    void validateForCreate_shouldThrowResourceConflict_whenCasNumberAlreadyTaken() {
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);
        when(reactantRepository.existsByCasNumber("64-17-5")).thenReturn(true);

        assertThrows(ResourceConflictException.class,
                () -> reactantValidator.validateForCreate(buildCreateDto("Ethanol", List.of("EtOH"), "64-17-5"), userId));
    }

    @Test
    void validateForCreate_shouldThrowResourceConflict_whenInChiKeyAlreadyExists() {
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);
        when(reactantRepository.existsByInChiKey("LFQSCWFLJHTTHZ-UHFFFAOYSA-N")).thenReturn(true);

        assertThrows(ResourceConflictException.class,
                () -> reactantValidator.validateForCreate(
                        buildCreateDtoWithInChiKey("Ethanol", List.of("EtOH"), "LFQSCWFLJHTTHZ-UHFFFAOYSA-N"), userId));
    }

    @Test
    void validateForCreate_shouldReturnResolvedReferences_whenInChiKeyIsNovel() {
        when(reactantRepository.hasNameConflict(anyCollection())).thenReturn(false);
        when(reactantRepository.existsByInChiKey("LFQSCWFLJHTTHZ-UHFFFAOYSA-N")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(chemicalTypeRepository.findById(chemicalTypeId)).thenReturn(Optional.of(chemicalType));

        ReactantValidator.ValidatedReferences result = reactantValidator.validateForCreate(
                buildCreateDtoWithInChiKey("Ethanol", List.of("EtOH"), "LFQSCWFLJHTTHZ-UHFFFAOYSA-N"), userId);

        assertNotNull(result.activeUser());
        assertNotNull(result.chemicalType());
    }

    @Test
    void validateForCreate_shouldThrowValidationException_whenAdditionalShortNamesDifferOnlyByCase() {
        assertThrows(ValidationException.class,
                () -> reactantValidator.validateForCreate(buildCreateDto("Hydrochloric Acid", List.of("HCl", "hcl"), null), userId));
    }

    @Test
    void validateForCreate_shouldThrowValidationException_whenAdditionalShortNamesContainsBlankEntry() {
        assertThrows(ValidationException.class,
                () -> reactantValidator.validateForCreate(buildCreateDto("Hydrochloric Acid", List.of("HCl", ""), null), userId));
    }

    @Test
    void validateForCreate_shouldThrowValidationException_whenShortNameIncludedInAdditionalShortNames() {
        assertThrows(ValidationException.class,
                () -> reactantValidator.validateForCreate(buildCreateDto("Ethanol", List.of("EtOH", "Ethanol"), null), userId));
    }

    // --- validateForUpdate: happy paths ----------------------------------

    @Test
    void validateForUpdate_shouldReturnNewChemicalType_whenChemicalTypeChanged() {
        UUID newChemicalTypeId = UUID.randomUUID();
        ChemicalType newChemicalType = new ChemicalType();
        newChemicalType.setId(newChemicalTypeId);

        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, newChemicalTypeId, null, null, null, null, null, null, null
        );

        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(reactantId))).thenReturn(false);
        when(chemicalTypeRepository.findById(newChemicalTypeId)).thenReturn(Optional.of(newChemicalType));

        ReactantValidator.ValidatedReferences result =
                reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant);

        assertNull(result.activeUser());
        assertEquals(newChemicalType, result.chemicalType());
    }

    @Test
    void validateForUpdate_shouldReturnNullChemicalType_whenChemicalTypeNotChanged() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, null, null, "New Name", null, null, null, null
        );

        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(reactantId))).thenReturn(false);

        ReactantValidator.ValidatedReferences result =
                reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant);

        assertNull(result.activeUser());
        assertNull(result.chemicalType());
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenNewChemicalTypeDoesNotExist() {
        UUID newChemicalTypeId = UUID.randomUUID();
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, newChemicalTypeId, null, null, null, null, null, null, null
        );

        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(reactantId))).thenReturn(false);
        when(chemicalTypeRepository.findById(newChemicalTypeId)).thenReturn(Optional.empty());

        assertThrows(UnprocessableEntityException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    // --- validateForUpdate: mandatory shortName/smiles blank-rejection ---

    @Test
    void validateForUpdate_shouldThrowValidationException_whenShortNameIsBlank() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, "", null, null, null, null, null, null
        );

        assertThrows(ValidationException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    @Test
    void validateForUpdate_shouldThrowValidationException_whenShortNameIsWhitespaceOnly() {
        // isBlank() must catch whitespace-only strings too, not just "".
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, "   ", null, null, null, null, null, null
        );

        assertThrows(ValidationException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    @Test
    void validateForUpdate_shouldThrowValidationException_whenSmilesIsBlank() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                "", null, null, null, null, null, null, null, null
        );

        assertThrows(ValidationException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    // --- validateForUpdate: additionalShortNames validation -------------

    @Test
    void validateForUpdate_shouldThrowValidationException_whenAdditionalShortNamesDifferOnlyByCase() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, null, List.of("HCl", "hcl"), null, null, null, null, null
        );

        assertThrows(ValidationException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    @Test
    void validateForUpdate_shouldThrowValidationException_whenAdditionalShortNamesContainsBlankEntry() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, null, List.of("EtOH", ""), null, null, null, null, null
        );

        assertThrows(ValidationException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    @Test
    void validateForUpdate_shouldThrowValidationException_whenShortNameIncludedInAdditionalShortNames() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, "NewName", List.of("OtherName", "NewName"), null, null, null, null, null
        );

        assertThrows(ValidationException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    // --- validateForUpdate: short-name conflict --------------------------

    @Test
    void validateForUpdate_shouldThrowResourceConflict_whenShortNameConflictsWithAnotherReactant() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, "NewName", null, null, null, null, null, null
        );

        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(reactantId))).thenReturn(true);

        assertThrows(ResourceConflictException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    // --- validateForUpdate: CAS number conflict --------------------------

    @Test
    void validateForUpdate_shouldThrowResourceConflict_whenCasNumberChangedAndAlreadyTaken() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, null, null, null, "99-99-9", null, null, null
        );

        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(reactantId))).thenReturn(false);
        when(reactantRepository.existsByCasNumberAndIdNot("99-99-9", reactantId)).thenReturn(true);

        assertThrows(ResourceConflictException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    @Test
    void validateForUpdate_shouldSkipCasNumberConflictCheck_whenCasNumberUnchanged() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, null, null, null, "64-17-5", null, null, null
        );

        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(reactantId))).thenReturn(false);

        ReactantValidator.ValidatedReferences result =
                reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant);

        assertNotNull(result);
        verify(reactantRepository, never()).existsByCasNumberAndIdNot(any(), any());
    }

    // --- validateForUpdate: InChIKey conflict -----------------------------

    @Test
    void validateForUpdate_shouldThrowResourceConflict_whenInChiKeyChangedAndAlreadyTaken() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, null, null, null, null, "NEWINCHIKEY123456789", null, null
        );

        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(reactantId))).thenReturn(false);
        when(reactantRepository.existsByInChiKeyAndIdNot("NEWINCHIKEY123456789", reactantId)).thenReturn(true);

        assertThrows(ResourceConflictException.class,
                () -> reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant));
    }

    @Test
    void validateForUpdate_shouldSkipInChiKeyConflictCheck_whenInChiKeyUnchanged() {
        UpdateReactantDto patchDto = new UpdateReactantDto(
                null, null, null, null, null, null, "LFQSCWFLJHTTHZ-UHFFFAOYSA-N", null, null
        );

        when(reactantRepository.hasNameConflictExcludingId(anyCollection(), eq(reactantId))).thenReturn(false);

        ReactantValidator.ValidatedReferences result =
                reactantValidator.validateForUpdate(reactantId, patchDto, existingReactant);

        assertNotNull(result);
        verify(reactantRepository, never()).existsByInChiKeyAndIdNot(any(), any());
    }
}
