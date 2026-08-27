package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.dtos.reactants.UpdateReactantDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.ReactantMapper;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.services.ReactantService;
import com.reactur.backend.services.StructureService;
import com.reactur.backend.validators.ReactantValidator;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ReactantServiceImpl implements ReactantService {

    private final ReactantRepository reactantRepository;
    private final ReactantValidator reactantValidator;
    private final ReactantMapper reactantMapper;
    private final StructureService structureService;
    private final ReactantStructureRepository reactantStructureRepository;

    @Override
    @Transactional
    public ReactantDetailDto createReactant(CreateReactantDto saveReactant, String userId) {

        ReactantValidator.ValidatedReferences validated = reactantValidator.validateForCreate(saveReactant, userId);
        User activeUser = validated.activeUser();
        ChemicalType chemicalType = validated.chemicalType();

        Reactant reactant = Reactant.builder()
                .shortName(saveReactant.shortName())
                .smiles(saveReactant.smiles())
                .type(chemicalType)
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
        // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): Reactant now
        // carries a @Version field, so if a concurrent updateReactant call on this same row commits first, this
        // method's own save() below fails loudly with ObjectOptimisticLockingFailureException instead of silently
        // discarding the other request's changes to fields this method didn't touch.
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));

        ReactantValidator.ValidatedReferences validated = reactantValidator.validateForUpdate(uuid, patchDto, reactant);
        if (validated.chemicalType() != null) {
            reactant.setType(validated.chemicalType());
        }

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
        List<Reactant> reactants = reactantRepository.findAllByArchivedAtIsNull();
        Map<UUID, List<ReactantStructure>> structuresByReactantId = groupStructuresByReactantId(reactants);

        return reactants.stream()
                .map(r -> reactantMapper.toDto(r, determineAggregateFlag(structuresByReactantId.get(r.getId()))))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactantDto> getArchivedReactants() {
        List<Reactant> reactants = reactantRepository.findAllByArchivedAtIsNotNull();
        Map<UUID, List<ReactantStructure>> structuresByReactantId = groupStructuresByReactantId(reactants);

        return reactants.stream()
                .map(r -> reactantMapper.toDto(r, determineAggregateFlag(structuresByReactantId.get(r.getId()))))
                .toList();
    }

    @Override
    @Transactional
    public void archiveReactant(UUID uuid) {
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));

        reactant.setArchivedAt(Instant.now());
        // Reactant now carries a @Version field (see the Race condition inventory item in Software_todo.md). This
        // unconditional set-and-save has no existing-state check to protect it, so two concurrent archiveReactant
        // calls on the same reactant would otherwise have the second one fail on a version conflict even though
        // both wanted the exact same outcome (archived). Since that's a harmless double-submit, not a real
        // conflict, treat a lock failure here as "someone already archived it" rather than surfacing an error.
        try {
            reactantRepository.save(reactant);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.debug("Reactant {} was archived concurrently by another request; treating as already archived.", uuid);
        }
    }

    @Override
    @Transactional
    public ReactantDto restoreReactant(UUID uuid) {
        Reactant reactant = reactantRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));

        reactant.setArchivedAt(null);
        // Same benign-double-submit reasoning as archiveReactant above: a lock conflict here means someone else
        // already restored this exact reactant, not a real conflict, so re-read the current state instead of
        // failing.
        Reactant saved;
        try {
            saved = reactantRepository.save(reactant);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.debug("Reactant {} was restored concurrently by another request; using the current state.", uuid);
            saved = reactantRepository.findById(uuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Reactant with ID " + uuid + " not found"));
        }
        return reactantMapper.toDto(saved, determineAggregateFlag(uuid));
    }

    private StructureFlag determineAggregateFlag(UUID reactantId) {
        List<StructureDto> structures = structureService.getStructuresByReactant(reactantId);
        if (structures == null) return null;
        return determineAggregateFlagFromFlags(structures.stream().map(StructureDto::flag).toList());
    }

    /**
     * Batch-oriented counterpart of {@link #determineAggregateFlag(UUID)} used by
     * {@link #getAllReactants()} and {@link #getArchivedReactants()}: computes the aggregate flag
     * from a reactant's own pre-fetched sublist of {@link ReactantStructure}s (see
     * {@link #groupStructuresByReactantId(List)}) instead of issuing a per-reactant query.
     */
    private StructureFlag determineAggregateFlag(List<ReactantStructure> structures) {
        if (structures == null) return null;
        return determineAggregateFlagFromFlags(structures.stream().map(ReactantStructure::getFlag).toList());
    }

    /**
     * Priority logic shared by both {@link #determineAggregateFlag(UUID)} and
     * {@link #determineAggregateFlag(List)}: ACTIVE &gt; READY &gt; PENDING &gt; all-FAILED &gt;
     * REJECTED.
     */
    private StructureFlag determineAggregateFlagFromFlags(List<StructureFlag> flags) {
        if (flags == null || flags.isEmpty()) return null;

        if (flags.stream().anyMatch(f -> f == StructureFlag.ACTIVE)) return StructureFlag.ACTIVE;
        if (flags.stream().anyMatch(f -> f == StructureFlag.READY)) return StructureFlag.READY;
        if (flags.stream().anyMatch(f -> f == StructureFlag.PENDING)) return StructureFlag.PENDING;
        if (flags.stream().allMatch(f -> f == StructureFlag.FAILED)) return StructureFlag.FAILED;

        return StructureFlag.REJECTED;
    }

    /**
     * Batch-fetches all {@link ReactantStructure}s for the given reactants in a single query and
     * groups them by reactant ID, avoiding the N+1 pattern of calling
     * {@code structureService.getStructuresByReactant} once per reactant (which itself repeats a
     * now-redundant reactant-existence check for every call).
     */
    private Map<UUID, List<ReactantStructure>> groupStructuresByReactantId(List<Reactant> reactants) {
        List<UUID> reactantIds = reactants.stream().map(Reactant::getId).toList();
        if (reactantIds.isEmpty()) {
            return Map.of();
        }

        return reactantStructureRepository.findByReactant_IdIn(reactantIds).stream()
                .collect(Collectors.groupingBy(s -> s.getReactant().getId()));
    }
}
