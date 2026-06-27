package com.reactur.backend.services;

import com.reactur.backend.Validators.ReactantValidator;
import com.reactur.backend.dtos.ReactantDtos.CreateReactantDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.ReactantDtos.UpdateReactantDto;
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
    private final UserService userService;
    private final ReactantValidator reactantValidator;
    private final ReactantMapper reactantMapper;

    @Override
    @Transactional
    public ReactantDetailDto createReactant(CreateReactantDto saveReactant) {

        reactantValidator.validateForCreate(saveReactant);

        User activeUser = userService.getCurrentUser().orElseThrow(() -> new RuntimeException("Current User could not be found"));

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

        return reactantMapper.toDetailDto(reactantRepository.save(reactant));
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

        return reactantMapper.toDetailDto(reactantRepository.save(reactant));
    }

    @Override
    @Transactional(readOnly = true)
    public ReactantDetailDto getReactantDetail(UUID uuid) {
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));
        return reactantMapper.toDetailDto(reactant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactantDto> getAllReactants() {
        return reactantRepository.findAllByArchivedAtIsNull().stream()
                .map(reactantMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactantDto> getArchivedReactants() {
        return reactantRepository.findAllByArchivedAtIsNotNull().stream()
                .map(reactantMapper::toDto)
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
        return reactantMapper.toDto(reactantRepository.save(reactant));
    }
}
