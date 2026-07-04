package com.reactur.backend.services.implementations;

import com.reactur.backend.client.JobStatusResult;
import com.reactur.backend.client.StructureApiClient;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.StructureMapper;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.services.StructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StructureServiceImpl implements StructureService {

    private final ReactantStructureRepository structureRepository;
    private final ReactantRepository reactantRepository;
    private final StructureMapper structureMapper;
    private final StructureApiClient structureApiClient;

    @Override
    @Transactional
    public StructureDto uploadStructure(UUID reactantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        Reactant reactant = reactantRepository.findById(reactantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant not found with id: " + reactantId));

        List<ReactantStructure> existingStructures = structureRepository.findByReactant_Id(reactantId);
        if (!existingStructures.isEmpty()) {
            structureRepository.deleteAll(existingStructures);
            log.info("Deleted {} existing structures for reactant {} prior to manual upload.", existingStructures.size(), reactantId);
        }

        try {
            String structureContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            ReactantStructure structure = ReactantStructure.builder()
                    .reactant(reactant)
                    .fileName(file.getOriginalFilename())
                    .structure(structureContent)
                    .flag(StructureFlag.ACTIVE)
                    .build();

            ReactantStructure savedStructure = structureRepository.save(structure);
            log.info("Successfully uploaded and saved structure for reactant: {}", reactantId);

            return structureMapper.toDto(savedStructure);

        } catch (IOException e) {
            log.error("Failed to read structure file for reactant: {}", reactantId, e);
            throw new RuntimeException("Failed to process the uploaded file", e);
        }
    }

    @Override
    @Transactional
    public void generateStructureFromAPI(UUID reactantId) {
        Reactant reactant = reactantRepository.findById(reactantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant not found with id: " + reactantId));

        List<ReactantStructure> existing = structureRepository.findByReactant_Id(reactantId);

        boolean hasPending = existing.stream().anyMatch(s -> s.getFlag() == StructureFlag.PENDING);
        boolean hasReady = existing.stream().anyMatch(s -> s.getFlag() == StructureFlag.READY);
        boolean hasActive = existing.stream().anyMatch(s -> s.getFlag() == StructureFlag.ACTIVE);
        boolean hasRejected = existing.stream().anyMatch(s -> s.getFlag() == StructureFlag.REJECTED);

        if (hasPending || hasReady) {
            throw new ResourceConflictException("Cannot generate: A structure generation is already in progress or awaiting your selection.");
        }
        if (hasActive && hasRejected) {
            throw new ResourceConflictException("Cannot generate: Structures have already been generated and selected. Upload manually to overwrite.");
        }

        log.info("Triggering external API for reactant smiles: {}", reactant.getSmiles());

        String jobId = structureApiClient.triggerJob(reactant.getSmiles());

        ReactantStructure pendingStructure = ReactantStructure.builder()
                .reactant(reactant)
                .externalJobId(jobId)
                .flag(StructureFlag.PENDING)
                .build();

        structureRepository.save(pendingStructure);
        log.info("Saved PENDING structure entry with Job ID: {} for reactant: {}", jobId, reactantId);
    }

    @Override
    @Transactional
    public StructureDto selectStructure(UUID structureId) {
        ReactantStructure selectedStructure = structureRepository.findById(structureId)
                .orElseThrow(() -> new ResourceNotFoundException("Structure not found with id: " + structureId));

        if (selectedStructure.getFlag() == StructureFlag.ACTIVE) {
            log.info("Structure {} is already ACTIVE. No changes made.", structureId);
            return structureMapper.toDto(selectedStructure);
        }

        UUID reactantId = selectedStructure.getReactant().getId();
        List<ReactantStructure> allStructures = structureRepository.findByReactant_Id(reactantId);

        for (ReactantStructure structure : allStructures) {
            if (structure.getId().equals(structureId)) {
                structure.setFlag(StructureFlag.ACTIVE);
            } else {
                structure.setFlag(StructureFlag.REJECTED);
            }
        }

        structureRepository.saveAll(allStructures);

        log.info("Structure {} selected as ACTIVE for reactant: {}", structureId, reactantId);
        return structureMapper.toDto(selectedStructure);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructureDto> getStructuresByReactant(UUID reactantId) {
        reactantRepository.findById(reactantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant not found with id: " + reactantId));

        List<ReactantStructure> structures = structureRepository.findByReactant_Id(reactantId);

        return structures.stream()
                .map(structureMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    public void fetchPendingStructuresFromAPI() {
        List<ReactantStructure> pendingStructures = structureRepository.findByFlag(StructureFlag.PENDING);

        if (pendingStructures.isEmpty()) {
            return;
        }

        log.info("Found {} PENDING structure jobs. Polling external API...", pendingStructures.size());

        for (ReactantStructure pending : pendingStructures) {
            String jobId = pending.getExternalJobId();

            try {
                JobStatusResult result = structureApiClient.checkJobStatus(jobId);

                if (result.isFinished()) {
                    List<String> generatedStructures = result.structures();

                    if (generatedStructures != null && !generatedStructures.isEmpty()) {
                        log.info("Job {} succeeded. Received {} structures.", jobId, generatedStructures.size());

                        pending.setStructure(generatedStructures.getFirst());
                        pending.setFlag(StructureFlag.READY);
                        structureRepository.save(pending);

                        for (int i = 1; i < generatedStructures.size(); i++) {
                            ReactantStructure additionalStructure = ReactantStructure.builder()
                                    .reactant(pending.getReactant())
                                    .externalJobId(jobId)
                                    .structure(generatedStructures.get(i))
                                    .flag(StructureFlag.READY)
                                    .build();
                            structureRepository.save(additionalStructure);
                        }
                    } else {
                        log.warn("Job {} succeeded but returned no structures.", jobId);
                        pending.setFlag(StructureFlag.FAILED);
                        structureRepository.save(pending);
                    }

                } else if (result.isFailed()) {
                    log.error("External Job {} failed. Error: {}", jobId, result.errorMessage());
                    pending.setFlag(StructureFlag.FAILED);
                    structureRepository.save(pending);
                }

            } catch (Exception e) {
                log.error("Failed to check status for pending Job ID: {}", jobId, e);
            }
        }
    }
}
