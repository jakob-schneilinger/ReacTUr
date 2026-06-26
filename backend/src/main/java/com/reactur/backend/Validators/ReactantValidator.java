package com.reactur.backend.Validators;

import com.reactur.backend.dtos.ReactantDtos.CreateReactantDto;
import com.reactur.backend.dtos.ReactantDtos.UpdateReactantDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.repositories.ReactantRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@AllArgsConstructor
public class ReactantValidator {

    private final ReactantRepository reactantRepository;

    public void validateForCreate(CreateReactantDto saveReactant) {
        validateAdditionalShortNames(saveReactant.shortName(), saveReactant.additionalShortNames());

        Set<String> namesToCheck = collectNames(saveReactant.shortName(), saveReactant.additionalShortNames());

        if (reactantRepository.hasNameConflict(namesToCheck)) {
            throw new ResourceConflictException("One of the given short names is already used by another reactant");
        }

        if (saveReactant.casNumber() != null && !saveReactant.casNumber().isBlank()) {
            if (reactantRepository.existsByCasNumber(saveReactant.casNumber())) {
                throw new ResourceConflictException("CAS number " + saveReactant.casNumber() + " is already taken");
            }
        }

        if (saveReactant.inChiKey() != null && !saveReactant.inChiKey().isBlank()) {
            if (reactantRepository.existsByInChiKey(saveReactant.inChiKey())) {
                throw new ResourceConflictException("inChiKey " + saveReactant.inChiKey() + " is already taken");
            }
        }
    }

    public void validateForUpdate(UUID uuid, UpdateReactantDto patchDto, Reactant existingReactant) {
        String effectiveShortName = patchDto.shortName() != null ? patchDto.shortName() : existingReactant.getShortName();
        List<String> effectiveAdditionalNames = patchDto.additionalShortNames() != null ? patchDto.additionalShortNames() : existingReactant.getAdditionalShortNames();

        if (patchDto.additionalShortNames() != null) {
            validateAdditionalShortNames(patchDto.shortName(), patchDto.additionalShortNames());
        }

        Set<String> namesToCheck = collectNames(effectiveShortName, effectiveAdditionalNames);

        if (reactantRepository.hasNameConflictExcludingId(namesToCheck, uuid)) {
            throw new ResourceConflictException("One of the short names is already being used by another reactant");
        }

        if (patchDto.casNumber() != null && !patchDto.casNumber().equals(existingReactant.getCasNumber()) && !patchDto.casNumber().isBlank()) {
            if (reactantRepository.existsByCasNumberAndIdNot(patchDto.casNumber(), uuid)) {
                throw new ResourceConflictException("CAS number " + patchDto.casNumber() + " is already taken");
            }
        }

        if (patchDto.inChiKey() != null && !patchDto.inChiKey().equals(existingReactant.getInChiKey()) && !patchDto.inChiKey().isBlank()) {
            if (reactantRepository.existsByInChiKeyAndIdNot(patchDto.inChiKey(), uuid)) {
                throw new ResourceConflictException("inChiKey " + patchDto.inChiKey() + " is already taken");
            }
        }
    }

    private void validateAdditionalShortNames(String shortName, List<String> additionalShortNames) {
        if (additionalShortNames == null || additionalShortNames.isEmpty()) {
            return;
        }

        boolean hasBlankNames = additionalShortNames.stream()
                .anyMatch(name -> name == null || name.isBlank());
        if (hasBlankNames) {
            throw new IllegalArgumentException("The 'additionalShortNames' list must not contain empty or blank entries.");
        }

        long uniqueNamesCount = additionalShortNames.stream().distinct().count();
        if (uniqueNamesCount != additionalShortNames.size()) {
            throw new IllegalArgumentException("The 'additionalShortNames' list contains duplicate entries.");
        }

        if (shortName != null && additionalShortNames.contains(shortName)) {
            throw new IllegalArgumentException("The 'shortName' cannot be included in 'additionalShortNames'.");
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