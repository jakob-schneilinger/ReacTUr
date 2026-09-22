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
     *
     * @param scientist  the resolved scientist
     * @param reactantA  the resolved first reactant
     * @param reactantB  the resolved second reactant
     * @param solvent    the resolved solvent
     * @param instrument the resolved instrument, or {@code null} if none was provided
     * @param modality   the resolved modality, or {@code null} if none was provided
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

    /**
     * Validates a {@link SaveExperimentDto} before a new experiment is created: verifies the
     * scientist, both reactants, and the referenced solvent/instrument/modality all exist, and
     * that reactant A and B are not the same reactant.
     *
     * @param experimentDto the DTO to validate
     * @return the resolved entities, for reuse without re-fetching by ID
     * @throws UnprocessableEntityException if any referenced entity does not exist, or a
     *                                      reactant lacks an ACTIVE 3D structure
     * @throws ValidationException          if reactant A and B are the same reactant
     */
    public ValidatedReferences validateForCreate(SaveExperimentDto experimentDto) {
        return validate(experimentDto, "create", "Reactants A and B cannot be the same.");
    }

    /**
     * Same validation as {@link #validateForCreate(SaveExperimentDto)}, for an experiment update.
     *
     * @param experimentDto the DTO to validate
     * @return the resolved entities, for reuse without re-fetching by ID
     * @throws UnprocessableEntityException if any referenced entity does not exist, or a
     *                                      reactant lacks an ACTIVE 3D structure
     * @throws ValidationException          if reactant A and B are the same reactant
     */
    public ValidatedReferences validateForUpdate(SaveExperimentDto experimentDto) {
        return validate(experimentDto, "update", "Reactant A and B cannot be the same in an update.");
    }

    /**
     * Shared implementation for {@link #validateForCreate(SaveExperimentDto)} and {@link
     * #validateForUpdate(SaveExperimentDto)}.
     * <p>
     * Both reactants are fetched with a pessimistic write lock, held until the enclosing
     * transaction commits, so that a concurrent {@code StructureServiceImpl.uploadStructure} call
     * replacing one of these reactants' structures can't run between this method's
     * active-structure check and the experiment actually being saved.
     *
     * @param experimentDto       the DTO to validate
     * @param operation           a short label ("create" or "update") used in the log message if
     *                            the same-reactant check fails
     * @param sameReactantMessage the exception message to use if reactant A and B are the same
     * @return the resolved entities, for reuse without re-fetching by ID
     */
    private ValidatedReferences validate(SaveExperimentDto experimentDto, String operation, String sameReactantMessage) {
        User scientist = userRepository.findById(experimentDto.metadata().scientist())
                .orElseThrow(() -> new UnprocessableEntityException(
                        "Scientist with ID " + experimentDto.metadata().scientist() + " not found."
                ));

        // Pessimistic write lock, held until this transaction commits. Blocks a concurrent request
        // from deleting or replacing this reactant's active structure while validation is in progress.
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
     * @throws UnprocessableEntityException if the reactant has no ACTIVE 3D structure
     */
    private void validateReactantHasActiveStructure(Reactant reactant, String label) {
        // The write lock taken when this reactant was fetched above is still held here.
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
