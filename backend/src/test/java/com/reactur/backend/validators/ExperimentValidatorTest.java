package com.reactur.backend.validators;

import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exeptions.UnprocessableEntityException;
import com.reactur.backend.exeptions.ValidationException;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExperimentValidator}, covering each of the 7 semantic validation rules
 * extracted from {@code ExperimentServiceImpl} (scientist existence, reactant A/B existence,
 * reactant A != B, active-structure requirement for both reactants, solvent existence, and the
 * optional instrument/modality existence checks).
 */
@ExtendWith(MockitoExtension.class)
class ExperimentValidatorTest {

    @Mock private UserRepository userRepository;
    @Mock private ReactantRepository reactantRepository;
    @Mock private SolventRepository solventRepository;
    @Mock private InstrumentRepository instrumentRepository;
    @Mock private ModalityRepository modalityRepository;
    @Mock private ReactantStructureRepository structureRepository;

    @InjectMocks
    private ExperimentValidator experimentValidator;

    private final String scientistId = "scientist-123";
    private final UUID reactantAId = randomUUID();
    private final UUID reactantBId = randomUUID();
    private final UUID solventId = randomUUID();
    private final UUID instrumentId = randomUUID();
    private final UUID modalityId = randomUUID();

    private User scientist;
    private Reactant reactantA;
    private Reactant reactantB;
    private Solvent solvent;
    private Instrument instrument;
    private Modality modality;

    @BeforeEach
    void setUp() {
        scientist = new User();
        scientist.setId(scientistId);

        reactantA = new Reactant();
        reactantA.setId(reactantAId);

        reactantB = new Reactant();
        reactantB.setId(reactantBId);

        solvent = new Solvent();
        solvent.setId(solventId);

        instrument = new Instrument();
        instrument.setId(instrumentId);

        modality = new Modality();
        modality.setId(modalityId);
    }

    private SaveExperimentDto buildDto(UUID reactantA, UUID reactantB, UUID instrument, UUID modality) {
        SaveMetadataDto metadata = new SaveMetadataDto(
                LocalDate.now().minusDays(1),
                scientistId,
                25.0,
                solventId,
                1.5,
                2.0,
                instrument,
                modality,
                450.0,
                "Test"
        );
        return new SaveExperimentDto(
                reactantA, reactantB, metadata,
                new MeasurementDataDto(1.2, 5.5, 0.1, false),
                List.of("10.1000/rxn123")
        );
    }

    /** Stubs all repositories with valid, matching entities for the "happy path". */
    private void stubAllValid() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));
        when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
        when(modalityRepository.findById(modalityId)).thenReturn(Optional.of(modality));
    }

    // --- Happy paths -----------------------------------------------------

    @Test
    void validateForCreate_shouldReturnResolvedReferences_whenAllReferencesValid() {
        stubAllValid();

        ExperimentValidator.ValidatedReferences result =
                experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId));

        assertEquals(scientist, result.scientist());
        assertEquals(reactantA, result.reactantA());
        assertEquals(reactantB, result.reactantB());
        assertEquals(solvent, result.solvent());
        assertEquals(instrument, result.instrument());
        assertEquals(modality, result.modality());
    }

    @Test
    void validateForUpdate_shouldReturnResolvedReferences_whenAllReferencesValid() {
        stubAllValid();

        ExperimentValidator.ValidatedReferences result =
                experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId));

        assertEquals(scientist, result.scientist());
        assertEquals(reactantA, result.reactantA());
        assertEquals(reactantB, result.reactantB());
        assertEquals(solvent, result.solvent());
        assertEquals(instrument, result.instrument());
        assertEquals(modality, result.modality());
    }

    @Test
    void validateForCreate_shouldReturnNullInstrumentAndModality_whenNotProvided() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));

        ExperimentValidator.ValidatedReferences result =
                experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, null, null));

        assertNull(result.instrument());
        assertNull(result.modality());
    }

    // --- 1. Scientist existence -------------------------------------------

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenScientistNotFound() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertTrue(exception.getMessage().contains("Scientist with ID"));
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenScientistNotFound() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertTrue(exception.getMessage().contains("Scientist with ID"));
    }

    // --- 2. Reactant A / B existence --------------------------------------

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenReactantAMissing() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Reactant A does not exist.", exception.getMessage());
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenReactantAMissing() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Reactant A does not exist.", exception.getMessage());
    }

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenReactantBMissing() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Reactant B does not exist.", exception.getMessage());
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenReactantBMissing() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Reactant B does not exist.", exception.getMessage());
    }

    // --- 3. Reactant A and B must differ ----------------------------------

    @Test
    void validateForCreate_shouldThrowValidationException_whenReactantsAreTheSame() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantAId, instrumentId, modalityId)));

        assertEquals("Reactants A and B cannot be the same.", exception.getMessage());
    }

    @Test
    void validateForUpdate_shouldThrowValidationException_whenReactantsAreTheSame() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantAId, instrumentId, modalityId)));

        assertEquals("Reactant A and B cannot be the same in an update.", exception.getMessage());
    }

    // --- 4. Both reactants must have an ACTIVE structure ------------------

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenReactantAHasNoActiveStructure() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));
        when(structureRepository.findByReactant_Id(reactantAId)).thenReturn(List.of());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertTrue(exception.getMessage().contains("Reactant A"));
        assertTrue(exception.getMessage().contains("ACTIVE 3D structure"));
    }

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenReactantBHasNoActiveStructure() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(reactantAId)).thenReturn(List.of(activeStructure));
        when(structureRepository.findByReactant_Id(reactantBId)).thenReturn(List.of());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertTrue(exception.getMessage().contains("Reactant B"));
        assertTrue(exception.getMessage().contains("ACTIVE 3D structure"));
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenReactantAHasNoActiveStructure() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));
        when(structureRepository.findByReactant_Id(reactantAId)).thenReturn(List.of());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertTrue(exception.getMessage().contains("Reactant A"));
        assertTrue(exception.getMessage().contains("ACTIVE 3D structure"));
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenReactantBHasNoActiveStructure() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(reactantAId)).thenReturn(List.of(activeStructure));
        when(structureRepository.findByReactant_Id(reactantBId)).thenReturn(List.of());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertTrue(exception.getMessage().contains("Reactant B"));
        assertTrue(exception.getMessage().contains("ACTIVE 3D structure"));
    }

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenReactantStructureIsNotActive() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure inactiveStructure = new ReactantStructure();
        inactiveStructure.setFlag(StructureFlag.REJECTED);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(inactiveStructure));

        assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenReactantStructureIsNotActive() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure inactiveStructure = new ReactantStructure();
        inactiveStructure.setFlag(StructureFlag.REJECTED);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(inactiveStructure));

        assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));
    }

    // --- 5. Solvent existence ----------------------------------------------

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenSolventNotFound() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Solvent does not exist.", exception.getMessage());
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenSolventNotFound() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Solvent does not exist.", exception.getMessage());
    }

    // --- 6. Instrument existence (optional) ---------------------------------

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenInstrumentProvidedButNotFound() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));
        when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Instrument does not exist.", exception.getMessage());
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenInstrumentProvidedButNotFound() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));
        when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Instrument does not exist.", exception.getMessage());
    }

    @Test
    void validateForCreate_shouldSkipInstrumentLookup_whenInstrumentNotProvided() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));
        when(modalityRepository.findById(modalityId)).thenReturn(Optional.of(modality));

        ExperimentValidator.ValidatedReferences result =
                experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, null, modalityId));

        assertNull(result.instrument());
        assertNotNull(result.modality());
    }

    @Test
    void validateForUpdate_shouldSkipInstrumentLookup_whenInstrumentNotProvided() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));
        when(modalityRepository.findById(modalityId)).thenReturn(Optional.of(modality));

        ExperimentValidator.ValidatedReferences result =
                experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, null, modalityId));

        assertNull(result.instrument());
        assertNotNull(result.modality());
    }

    // --- 7. Modality existence (optional) -----------------------------------

    @Test
    void validateForCreate_shouldThrowUnprocessableEntity_whenModalityProvidedButNotFound() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));
        when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
        when(modalityRepository.findById(modalityId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Modality does not exist.", exception.getMessage());
    }

    @Test
    void validateForUpdate_shouldThrowUnprocessableEntity_whenModalityProvidedButNotFound() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));
        when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));
        when(modalityRepository.findById(modalityId)).thenReturn(Optional.empty());

        UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class,
                () -> experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, modalityId)));

        assertEquals("Modality does not exist.", exception.getMessage());
    }

    @Test
    void validateForCreate_shouldSkipModalityLookup_whenModalityNotProvided() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));
        when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));

        ExperimentValidator.ValidatedReferences result =
                experimentValidator.validateForCreate(buildDto(reactantAId, reactantBId, instrumentId, null));

        assertNotNull(result.instrument());
        assertNull(result.modality());
    }

    @Test
    void validateForUpdate_shouldSkipModalityLookup_whenModalityNotProvided() {
        when(userRepository.findById(scientistId)).thenReturn(Optional.of(scientist));
        when(reactantRepository.findByIdForUpdate(reactantAId)).thenReturn(Optional.of(reactantA));
        when(reactantRepository.findByIdForUpdate(reactantBId)).thenReturn(Optional.of(reactantB));

        ReactantStructure activeStructure = new ReactantStructure();
        activeStructure.setFlag(StructureFlag.ACTIVE);
        when(structureRepository.findByReactant_Id(any())).thenReturn(List.of(activeStructure));

        when(solventRepository.findById(solventId)).thenReturn(Optional.of(solvent));
        when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));

        ExperimentValidator.ValidatedReferences result =
                experimentValidator.validateForUpdate(buildDto(reactantAId, reactantBId, instrumentId, null));

        assertNotNull(result.instrument());
        assertNull(result.modality());
    }
}
