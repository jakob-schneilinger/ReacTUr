package com.reactur.backend.services;

import com.reactur.backend.dtos.ReactantDtos.CreateReactantDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.ReactantDtos.UpdateReactantDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.ReactantRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReactantServiceImpl implements ReactantService {

    private final ReactantRepository reactantRepository;

    @Override
    @Transactional
    public ReactantDetailDto createReactant(CreateReactantDto saveReactant) {
        Set<String> namesToCheck = collectNames(saveReactant.shortname(), saveReactant.additionalShortNames());

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
                throw new ResourceConflictException("inChiKey number " + saveReactant.casNumber() + " is already taken");
            }
        }

        //TODO: Here should be a method to get current User
        User fallbackUser = new User();
        fallbackUser.setId("526a3632-b1cd-4c04-b9bc-b4e49453cbb6");

        //TODO: Check if user is admin?

        Reactant reactant = Reactant.builder()
                .shortName(saveReactant.shortname())
                .smiles(saveReactant.smiles())
                .inChiKey(saveReactant.inChiKey() != null ? saveReactant.inChiKey() : "")
                .chemicalName(saveReactant.chemicalName() != null ? saveReactant.chemicalName() : "")
                .casNumber(saveReactant.casNumber() != null ? saveReactant.casNumber() : "")
                .comment(saveReactant.comment() != null ? saveReactant.comment() : "")
                .additionalShortNames(saveReactant.additionalShortNames() != null ? new ArrayList<>(saveReactant.additionalShortNames()) : new ArrayList<>())
                .references(saveReactant.references() != null ? new ArrayList<>(saveReactant.references()) : new ArrayList<>())
                .createdBy(fallbackUser)
                .build();

        return mapToDetailDto(reactantRepository.save(reactant));
    }

    @Override
    @Transactional
    public ReactantDetailDto updateReactant(UUID uuid, UpdateReactantDto patchDto) {
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));

        String effectiveShortName = patchDto.shortname() != null ? patchDto.shortname() : reactant.getShortName();
        List<String> effectiveAdditionalNames = patchDto.additionalShortNames() != null ? patchDto.additionalShortNames() : reactant.getAdditionalShortNames();

        Set<String> namesToCheck = collectNames(effectiveShortName, effectiveAdditionalNames);

        if (reactantRepository.hasNameConflictExcludingId(namesToCheck, uuid)) {
            throw new ResourceConflictException("One of the short names is already being used by another reactant");
        }

        if (patchDto.casNumber() != null && !patchDto.casNumber().equals(reactant.getCasNumber()) && !patchDto.casNumber().isBlank()) {
            if (reactantRepository.existsByCasNumberAndIdNot(patchDto.casNumber(), uuid)) {
                throw new ResourceConflictException("CAS number " + patchDto.casNumber() + " is already taken");
            }
        }

        if (patchDto.inChiKey() != null && !patchDto.inChiKey().equals(reactant.getInChiKey()) && !patchDto.inChiKey().isBlank()) {
            if (reactantRepository.existsByCasNumberAndIdNot(patchDto.inChiKey(), uuid)) {
                throw new ResourceConflictException("inChiKey " + patchDto.inChiKey() + " is already taken");
            }
        }

        // PATCH: Update if not null. "" clears strings, [] empties lists.
        if (patchDto.shortname() != null) reactant.setShortName(patchDto.shortname());
        if (patchDto.smiles() != null) reactant.setSmiles(patchDto.smiles());
        if (patchDto.inChiKey() != null) reactant.setInChiKey(patchDto.inChiKey());
        if (patchDto.chemicalName() != null) reactant.setChemicalName(patchDto.chemicalName());
        if (patchDto.casNumber() != null) reactant.setCasNumber(patchDto.casNumber());
        if (patchDto.comment() != null) reactant.setComment(patchDto.comment());

        if (patchDto.additionalShortNames() != null) {
            reactant.setAdditionalShortNames(new ArrayList<>(patchDto.additionalShortNames()));
        }

        if (patchDto.references() != null) {
            reactant.setReferences(new ArrayList<>(patchDto.references()));
        }

        return mapToDetailDto(reactantRepository.save(reactant));
    }

    @Override
    @Transactional(readOnly = true)
    public ReactantDetailDto getReactantDetail(UUID uuid) {
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));
        return mapToDetailDto(reactant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactantDto> getAllReactants() {
        return reactantRepository.findAllByArchivedAtIsNull().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactantDto> getArchivedReactants() {
        return reactantRepository.findAllByArchivedAtIsNotNull().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void archiveReactant(UUID uuid) {
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));

        reactant.setArchivedAt(Instant.now());
        reactantRepository.save(reactant);
    }

    @Override
    @Transactional
    public ReactantDto restoreReactant(UUID uuid) {
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));

        reactant.setArchivedAt(null);
        return mapToDto(reactantRepository.save(reactant));
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

    private ReactantDetailDto mapToDetailDto(Reactant reactant) {
        return new ReactantDetailDto(
                reactant.getId(),
                reactant.getSmiles(),
                null, // TODO: Get Type method should be inserted here !
                reactant.getShortName(),
                reactant.getAdditionalShortNames(),
                reactant.getChemicalName(),
                reactant.getCasNumber(),
                reactant.getInChiKey(),
                reactant.getComment(),
                reactant.getReferences(),
                reactant.getArchivedAt()
        );
    }

    private ReactantDto mapToDto(Reactant reactant) {
        return new ReactantDto(
                reactant.getId(),
                reactant.getSmiles(),
                null, // TODO: Get Type method should be inserted here !
                reactant.getShortName(),
                reactant.getChemicalName()
        );
    }
}
