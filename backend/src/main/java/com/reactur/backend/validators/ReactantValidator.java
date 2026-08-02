package com.reactur.backend.validators;

import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.UpdateReactantDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.exeptions.ValidationException;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.ChemicalTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Validates the semantic (conflict and reference-existence) correctness of the create/update
 * DTOs for a {@link Reactant}, shared between {@code createReactant} and {@code updateReactant}
 * in {@code ReactantServiceImpl}.
 * <p>
 * This validator's methods also return the resolved entities via {@link ValidatedReferences}.
 */
@Component
@AllArgsConstructor
public class ReactantValidator {

    private final ReactantRepository reactantRepository;
    private final UserRepository userRepository;
    private final ChemicalTypeRepository chemicalTypeRepository;

    /**
     * Bundles the entities resolved while validating a reactant create/update so that
     * callers can reuse them without re-fetching by ID.
     */
    public record ValidatedReferences(User activeUser, ChemicalType chemicalType) {
    }

    public ValidatedReferences validateForCreate(CreateReactantDto saveReactant, String userId) {
        List<String> validationErrors = new ArrayList<>();
        validateAdditionalShortNames(saveReactant.shortName(), saveReactant.additionalShortNames(), validationErrors);

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }

        List<String> conflictErrors = new ArrayList<>();
        Set<String> namesToCheck = collectNames(saveReactant.shortName(), saveReactant.additionalShortNames());

        if (!namesToCheck.isEmpty() && reactantRepository.hasNameConflict(namesToCheck)) {
            conflictErrors.add("One of the given short names is already used by another reactant.");
        }

        if (saveReactant.casNumber() != null && !saveReactant.casNumber().isBlank()) {
            if (reactantRepository.existsByCasNumber(saveReactant.casNumber())) {
                conflictErrors.add("CAS number " + saveReactant.casNumber() + " is already taken.");
            }
        }

        if (saveReactant.inChiKey() != null && !saveReactant.inChiKey().isBlank()) {
            if (reactantRepository.existsByInChiKey(saveReactant.inChiKey())) {
                conflictErrors.add("inChiKey " + saveReactant.inChiKey() + " is already taken.");
            }
        }

        if (!conflictErrors.isEmpty()) {
            throw new ResourceConflictException(conflictErrors);
        }

        User activeUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Current User could not be found"));

        ChemicalType chemicalType = chemicalTypeRepository.findById(saveReactant.chemicalType())
                .orElseThrow(() -> new ResourceNotFoundException("ChemicalType with ID " + saveReactant.chemicalType() + " not found"));

        return new ValidatedReferences(activeUser, chemicalType);
    }

    public ValidatedReferences validateForUpdate(UUID uuid, UpdateReactantDto patchDto, Reactant existingReactant) {
        List<String> validationErrors = new ArrayList<>();

        if (patchDto.additionalShortNames() != null) {
            validateAdditionalShortNames(patchDto.shortName(), patchDto.additionalShortNames(), validationErrors);
        }

        if (patchDto.shortName() != null && patchDto.shortName().isBlank()) {
            validationErrors.add("The 'shortName' must not be blank.");
        }

        if (patchDto.smiles() != null && patchDto.smiles().isBlank()) {
            validationErrors.add("The 'smiles' must not be blank.");
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }

        List<String> conflictErrors = new ArrayList<>();
        String effectiveShortName = patchDto.shortName() != null ? patchDto.shortName() : existingReactant.getShortName();
        List<String> effectiveAdditionalNames = patchDto.additionalShortNames() != null ? patchDto.additionalShortNames() : existingReactant.getAdditionalShortNames();

        Set<String> namesToCheck = collectNames(effectiveShortName, effectiveAdditionalNames);

        if (!namesToCheck.isEmpty() && reactantRepository.hasNameConflictExcludingId(namesToCheck, uuid)) {
            conflictErrors.add("One of the short names is already being used by another reactant.");
        }

        if (patchDto.casNumber() != null && !patchDto.casNumber().equals(existingReactant.getCasNumber()) && !patchDto.casNumber().isBlank()) {
            if (reactantRepository.existsByCasNumberAndIdNot(patchDto.casNumber(), uuid)) {
                conflictErrors.add("CAS number " + patchDto.casNumber() + " is already taken.");
            }
        }

        if (patchDto.inChiKey() != null && !patchDto.inChiKey().equals(existingReactant.getInChiKey()) && !patchDto.inChiKey().isBlank()) {
            if (reactantRepository.existsByInChiKeyAndIdNot(patchDto.inChiKey(), uuid)) {
                conflictErrors.add("inChiKey " + patchDto.inChiKey() + " is already taken.");
            }
        }

        if (!conflictErrors.isEmpty()) {
            throw new ResourceConflictException(conflictErrors);
        }

        ChemicalType chemicalType = null;
        if (patchDto.chemicalType() != null) {
            chemicalType = chemicalTypeRepository.findById(patchDto.chemicalType())
                    .orElseThrow(() -> new ResourceNotFoundException("ChemicalType with ID " + patchDto.chemicalType() + " not found"));
        }

        return new ValidatedReferences(null, chemicalType);
    }
    
    private void validateAdditionalShortNames(String shortName, List<String> additionalShortNames, List<String> validationErrors) {
        if (additionalShortNames == null || additionalShortNames.isEmpty()) {
            return;
        }

        boolean hasBlankNames = additionalShortNames.stream()
                .anyMatch(name -> name == null || name.isBlank());
        if (hasBlankNames) {
            validationErrors.add("The 'additionalShortNames' list must not contain empty or blank entries.");
        }

        long uniqueNamesCount = additionalShortNames.stream()
                .map(name -> name == null ? null : name.toLowerCase())
                .distinct()
                .count();
        if (uniqueNamesCount != additionalShortNames.size()) {
            validationErrors.add("The 'additionalShortNames' list contains duplicate entries.");
        }

        if (shortName != null && additionalShortNames.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(shortName))) {
            validationErrors.add("The 'shortName' cannot be included in 'additionalShortNames'.");
        }
    }

    /**
     * Collects the short names to check for conflicts, lowercased so they satisfy the
     * case-insensitive contract of {@link ReactantRepository#hasNameConflict(java.util.Collection)}
     * and {@link ReactantRepository#hasNameConflictExcludingId(java.util.Collection, java.util.UUID)}.
     */
    private Set<String> collectNames(String primary, List<String> additional) {
        Set<String> names = new HashSet<>();
        if (primary != null && !primary.isBlank()) {
            names.add(primary.toLowerCase());
        }
        if (additional != null) {
            additional.stream()
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .forEach(names::add);
        }
        return names;
    }
}
