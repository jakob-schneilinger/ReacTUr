package com.reactur.backend.validators;

import com.reactur.backend.dtos.experiments.SaveExperimentDto;
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
import lombok.AllArgsConstructor;
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
        return validate(experimentDto, "Reactants A and B cannot be the same.");
    }

    public ValidatedReferences validateForUpdate(SaveExperimentDto experimentDto) {
        return validate(experimentDto, "Reactant A and B cannot be the same in an update.");
    }

    private ValidatedReferences validate(SaveExperimentDto experimentDto, String sameReactantMessage) {
        User scientist = userRepository.findById(experimentDto.metadata().scientist())
                .orElseThrow(() -> new UnprocessableEntityException(
                        "Scientist with ID " + experimentDto.metadata().scientist() + " not found."
                ));

        Reactant reactantA = reactantRepository.findById(experimentDto.reactantA())
                .orElseThrow(() -> new UnprocessableEntityException("Reactant A does not exist."));

        Reactant reactantB = reactantRepository.findById(experimentDto.reactantB())
                .orElseThrow(() -> new UnprocessableEntityException("Reactant B does not exist."));

        if (reactantA.getId().equals(reactantB.getId())) {
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
        // TODO-CLAUDE (race condition inventory, Family 1 - fixable without migration tooling, see
        // docs/Software_todo.md "Race condition inventory" under the ReactantRepository.hasNameConflict item): this check
        // targets one specific, already-existing Reactant row (by id), so it's cheaply fixable in principle - but note
        // it's a genuine cross-service race: StructureServiceImpl.uploadStructure can delete/replace this exact
        // reactant's structures (including its ACTIVE one) between this check passing and the caller's Experiment save
        // committing. Closing it would mean taking a @Lock(LockModeType.PESSIMISTIC_WRITE) on the Reactant row here AND
        // having StructureServiceImpl.uploadStructure take the same lock before its deleteAll - a lock that needs to be
        // coordinated across two different service classes, not just added locally in one place.
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
