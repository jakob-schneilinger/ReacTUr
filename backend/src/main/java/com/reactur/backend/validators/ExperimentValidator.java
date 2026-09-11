package com.reactur.backend.validators;

import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exceptions.UnprocessableEntityException;
import com.reactur.backend.exceptions.ValidationException;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates the semantic (reference-existence and business-rule) correctness of a
 * {@link SaveExperimentDto}, shared between {@code createExperiment} and
 * {@code updateExperiment} in {@code ExperimentServiceImpl}.
 * <p>
 * This validator's methods also return the resolved entities via
 * {@link ValidatedReferences}. Since every one of these entities must be looked up here anyway
 * to validate its existence, returning them lets the service reuse them directly instead of
 * issuing the same {@code findById} queries a second time.
 */
@Slf4j
@Component
@AllArgsConstructor
public class ExperimentValidator {

    private final UserRepository userRepository;
    private final ReactantRepository reactantRepository;
    private final SolventRepository solventRepository;
    private final InstrumentRepository instrumentRepository;
    private final ModalityRepository modalityRepository;
    private final ReactantStructureRepository structureRepository;

    /**
     * Bundles the entities resolved while validating a {@link SaveExperimentDto} so that
     * callers can reuse them without re-fetching by ID.
     */
    public record ValidatedReferences(
            User scientist,
            Reactant reactantA,
            Reactant reactantB,
            Solvent solvent,
            Instrument instrument,
            Modality modality
    ) {
    }

    public ValidatedReferences validateForCreate(SaveExperimentDto experimentDto) {
        return validate(experimentDto, "create", "Reactants A and B cannot be the same.");
    }

    public ValidatedReferences validateForUpdate(SaveExperimentDto experimentDto) {
        return validate(experimentDto, "update", "Reactant A and B cannot be the same in an update.");
    }

    private ValidatedReferences validate(SaveExperimentDto experimentDto, String operation, String sameReactantMessage) {
        User scientist = userRepository.findById(experimentDto.metadata().scientist())
                .orElseThrow(() -> new UnprocessableEntityException(
                        "Scientist with ID " + experimentDto.metadata().scientist() + " not found."
                ));

        // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): these used to be
        // plain findById calls. Now they take a pessimistic write lock on each reactant row, held until the
        // caller's transaction (ExperimentServiceImpl.createExperiment/updateExperiment) commits, so a concurrent
        // StructureServiceImpl.uploadStructure call for the SAME reactant (which takes the same lock before its own
        // deleteAll) blocks until this transaction finishes, instead of being able to delete/replace this
        // reactant's ACTIVE structure in the window between this validation and the experiment being saved.
        Reactant reactantA = reactantRepository.findByIdForUpdate(experimentDto.reactantA())
                .orElseThrow(() -> new UnprocessableEntityException("Reactant A does not exist."));

        Reactant reactantB = reactantRepository.findByIdForUpdate(experimentDto.reactantB())
                .orElseThrow(() -> new UnprocessableEntityException("Reactant B does not exist."));

        if (reactantA.getId().equals(reactantB.getId())) {
            log.warn("Rejected experiment {} attempt: reactant A and B are the same reactant (ID: {}).",
                    operation, reactantA.getId());
            throw new ValidationException(sameReactantMessage);
        }

        validateReactantHasActiveStructure(reactantA, "A");
        validateReactantHasActiveStructure(reactantB, "B");

        Solvent solvent = solventRepository.findById(experimentDto.metadata().solvent())
                .orElseThrow(() -> new UnprocessableEntityException("Solvent does not exist."));

        Instrument instrument = null;
        if (experimentDto.metadata().instrument() != null) {
            instrument = instrumentRepository.findById(experimentDto.metadata().instrument())
                    .orElseThrow(() -> new UnprocessableEntityException("Instrument does not exist."));
        }

        Modality modality = null;
        if (experimentDto.metadata().modality() != null) {
            modality = modalityRepository.findById(experimentDto.metadata().modality())
                    .orElseThrow(() -> new UnprocessableEntityException("Modality does not exist."));
        }

        return new ValidatedReferences(scientist, reactantA, reactantB, solvent, instrument, modality);
    }

    /**
     * Validates that a given Reactant has at least one associated ReactantStructure
     * with the flag set to ACTIVE via the repository.
     *
     * @param reactant The Reactant to validate.
     * @param label A label (e.g., "A" or "B") for clearer error messages.
     */
    private void validateReactantHasActiveStructure(Reactant reactant, String label) {
        // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): the reactant
        // passed in here was already fetched via findByIdForUpdate above, so the pessimistic write lock on its row
        // is already held for the rest of this transaction by the time this method runs.
        List<ReactantStructure> structures = structureRepository.findByReactant_Id(reactant.getId());

        boolean hasActiveStructure = structures.stream()
                .anyMatch(structure -> structure.getFlag() == StructureFlag.ACTIVE);

        if (!hasActiveStructure) {
            throw new UnprocessableEntityException(
                    "Reactant " + label + " (ID: " + reactant.getId() + ") cannot be used. It does not have an ACTIVE 3D structure."
            );
        }
    }
}
