package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.ReactantService;
import com.reactur.backend.services.StructureService;
import com.reactur.backend.validators.ReactantValidator;
import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.UpdateReactantDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.ReactantMapper;
import com.reactur.backend.repositories.ReactantRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReactantServiceImpl implements ReactantService {

    private final ReactantRepository reactantRepository;
    private final UserRepository userRepository;
    private final ReactantValidator reactantValidator;
    private final ReactantMapper reactantMapper;
    private final StructureService structureService;

    @Override
    @Transactional
    public ReactantDetailDto createReactant(CreateReactantDto saveReactant, String userId) {

        reactantValidator.validateForCreate(saveReactant);

        User activeUser = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Current User could not be found"));

        Reactant reactant = Reactant.builder()
                .shortName(saveReactant.shortName())
                .smiles(saveReactant.smiles())
                .inChiKey(saveReactant.inChiKey() != null ? saveReactant.inChiKey() : "")
                .chemicalName(saveReactant.chemicalName() != null ? saveReactant.chemicalName() : "")
                .casNumber(saveReactant.casNumber() != null ? saveReactant.casNumber() : "")
                .comment(saveReactant.comment() != null ? saveReactant.comment() : "")
                .additionalShortNames(saveReactant.additionalShortNames() != null ? new ArrayList<>(saveReactant.additionalShortNames()) : new ArrayList<>())
                .references(saveReactant.references() != null ? new ArrayList<>(saveReactant.references()) : new ArrayList<>())
                .createdBy(activeUser)
                .build();

        Reactant saved = reactantRepository.save(reactant);
        return reactantMapper.toDetailDto(saved, List.of());
    }

    @Override
    @Transactional
    public ReactantDetailDto updateReactant(UUID uuid, UpdateReactantDto patchDto) {
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));

        reactantValidator.validateForUpdate(uuid, patchDto, reactant);

        // PATCH: Update if not null. "" clears strings, [] empties lists.
        if (patchDto.shortName() != null) reactant.setShortName(patchDto.shortName());
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

        Reactant saved = reactantRepository.save(reactant);
        List<StructureDto> structures = structureService.getStructuresByReactant(uuid);

        return reactantMapper.toDetailDto(saved, structures);
    }

    @Override
    @Transactional(readOnly = true)
    public ReactantDetailDto getReactantDetail(UUID uuid) {
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));

        List<StructureDto> structures = structureService.getStructuresByReactant(uuid);
        return reactantMapper.toDetailDto(reactant, structures);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactantDto> getAllReactants() {
        return reactantRepository.findAllByArchivedAtIsNull().stream()
                .map(r -> reactantMapper.toDto(r, determineAggregateFlag(r.getId())))
                .collect(Collectors.toList());    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactantDto> getArchivedReactants() {
        return reactantRepository.findAllByArchivedAtIsNotNull().stream()
                .map(r -> reactantMapper.toDto(r, determineAggregateFlag(r.getId())))
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
        Reactant saved = reactantRepository.save(reactant);
        return reactantMapper.toDto(saved, determineAggregateFlag(uuid));
    }

    private StructureFlag determineAggregateFlag(UUID reactantId) {
        List<StructureDto> structures = structureService.getStructuresByReactant(reactantId);
        if (structures == null || structures.isEmpty()) return null;

        if (structures.stream().anyMatch(s -> s.flag() == StructureFlag.ACTIVE)) return StructureFlag.ACTIVE;
        if (structures.stream().anyMatch(s -> s.flag() == StructureFlag.READY)) return StructureFlag.READY;
        if (structures.stream().anyMatch(s -> s.flag() == StructureFlag.PENDING)) return StructureFlag.PENDING;
        if (structures.stream().allMatch(s -> s.flag() == StructureFlag.FAILED)) return StructureFlag.FAILED;

        return StructureFlag.REJECTED;
    }
}
