package com.reactur.backend.validators;

import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.UpdateReactantDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ValidationException;
import com.reactur.backend.repositories.ReactantRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@AllArgsConstructor
public class ReactantValidator {

    private final ReactantRepository reactantRepository;

    public void validateForCreate(CreateReactantDto saveReactant) {
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
    }

    public void validateForUpdate(UUID uuid, UpdateReactantDto patchDto, Reactant existingReactant) {
        List<String> validationErrors = new ArrayList<>();

        if (patchDto.additionalShortNames() != null) {
            validateAdditionalShortNames(patchDto.shortName(), patchDto.additionalShortNames(), validationErrors);
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

        long uniqueNamesCount = additionalShortNames.stream().distinct().count();
        if (uniqueNamesCount != additionalShortNames.size()) {
            validationErrors.add("The 'additionalShortNames' list contains duplicate entries.");
        }

        if (shortName != null && additionalShortNames.contains(shortName)) {
            validationErrors.add("The 'shortName' cannot be included in 'additionalShortNames'.");
        }
    }

    private Set<String> collectNames(String primary, List<String> additional) {
        Set<String> names = new HashSet<>();
        if (primary != null && !primary.isBlank()) {
            names.add(primary);
        }
        if (additional != null) {
            names.addAll(additional);
        }
        return names;
    }
}