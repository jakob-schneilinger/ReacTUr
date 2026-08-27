package com.reactur.backend.services.implementations;

import com.reactur.backend.client.JobStatusResult;
import com.reactur.backend.client.StructureApiClient;
import com.reactur.backend.dtos.reactants.StructureDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exeptions.FileStorageException;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.StructureMapper;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.services.StructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StructureServiceImpl implements StructureService {

    private final ReactantStructureRepository structureRepository;
    private final ReactantRepository reactantRepository;
    private final StructureMapper structureMapper;
    private final StructureApiClient structureApiClient;

    /**
     * Basic upper bound on the size of a single structure payload accepted from the external
     * structure-generation API before persisting it (A08 - response content validation).
     * <p>
     * {@link com.reactur.backend.entities.ReactantStructure#getStructure()} is backed by a
     * Postgres {@code TEXT} column, which has no meaningful size limit of its own (~1GB hard
     * ceiling), so this is purely an application-level sanity guard rather than a mirror of a DB
     * constraint. Legitimate payloads here are XYZ/MOL-format 3D structures for the small organic
     * reactants this system deals with (roughly tens to a few thousand atoms); even a generous
     * ~10,000-atom XYZ file (at ~50-60 chars/atom line) is still well under 1MB. 1,000,000
     * characters gives about two orders of magnitude of headroom over any realistic legitimate
     * structure, while still catching obviously-wrong responses (HTML error pages, truncated or
     * corrupted payloads, runaway/garbage data, etc.) before they ever reach the database.
     */
    private static final int MAX_STRUCTURE_LENGTH = 1_000_000;

    /**
     * Provisional per-file size cap in bytes (default 1MB) for manually-uploaded structure files,
     * matching the limit the frontend's FileUploadComponent already enforces for reactant structure
     * uploads (see reactant-details-page.component.html's {@code [maxFileSizeMB]="1"}). This is NOT a
     * finalized product decision - see Software_todo.md item 14 - just a stop-gap so the backend
     * enforces something instead of relying solely on the frontend. Deployers can override via
     * {@code app.upload.structure-file.max-size-bytes} without a code change.
     * TODO: replace with a properly decided, tested limit (and matching file-type restrictions) once
     * product/Dennis decide the real policy - see Software_todo.md item 14.
     */
    @Value("${app.upload.structure-file.max-size-bytes:1048576}")
    private long maxStructureFileSizeBytes = 1_048_576L;

    /**
     * Poll interval, in milliseconds, between successive runs of {@link #fetchPendingStructuresFromAPI()}.
     */
    private static final long PENDING_STRUCTURE_POLL_INTERVAL_MS = 60_000;

    @Override
    @Transactional
    public StructureDto uploadStructure(UUID reactantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        if (file.getSize() > maxStructureFileSizeBytes) {
            throw new IllegalArgumentException("Uploaded file exceeds the maximum allowed size of " + maxStructureFileSizeBytes + " bytes.");
        }

        // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): this used to be
        // a plain findById. Now it takes a pessimistic write lock on the reactant row, held until this method's
        // transaction commits, so a concurrent uploadStructure/generateStructureFromAPI/selectStructure call for
        // the SAME reactant blocks here until this one finishes, instead of racing against the findByReactant_Id +
        // deleteAll below. Safe to hold across this method's critical section: it does local file-content parsing
        // only (MultipartFile.getBytes(), no network I/O), no S3/HTTP call in between.
        Reactant reactant = reactantRepository.findByIdForUpdate(reactantId)
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
            throw new FileStorageException("Failed to read the uploaded structure file for reactant: " + reactantId, e);
        }
    }

    @Override
    @Transactional
    public void generateStructureFromAPI(UUID reactantId) {
        Reactant reactant = reactantRepository.findById(reactantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant not found with id: " + reactantId));

        List<ReactantStructure> existing = structureRepository.findByReactant_Id(reactantId);

        // TODO-CLAUDE (race condition inventory, Family 2, see docs/Software_todo.md "Race condition inventory" under the
        // ReactantRepository.hasNameConflict item): this "no PENDING/READY job already exists for this reactant" check is
        // a table-wide query, not a lookup of one specific known row - the conflicting row (a PENDING job inserted by a
        // concurrent call) doesn't exist yet at check time. Two concurrent calls can both observe "no pending job" and
        // both insert a PENDING row plus trigger duplicate external generation jobs for the same reactant. Not fixable
        // via Java-side row locking alone; needs a DB constraint or SERIALIZABLE isolation + retry. Already accepted as an
        // explicitly-not-fixed gap (see docs/Software_todo.md's "Explicitly NOT fixed, on purpose" item).
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
        // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): ReactantStructure
        // now carries a @Version field, so if this exact row gets changed concurrently between this read and this
        // method's own save below, the save fails loudly with ObjectOptimisticLockingFailureException instead of
        // silently overwriting stale data.
        ReactantStructure selectedStructure = structureRepository.findById(structureId)
                .orElseThrow(() -> new ResourceNotFoundException("Structure not found with id: " + structureId));

        if (selectedStructure.getFlag() == StructureFlag.ACTIVE) {
            log.info("Structure {} is already ACTIVE. No changes made.", structureId);
            return structureMapper.toDto(selectedStructure);
        }

        if (selectedStructure.getFlag() == StructureFlag.PENDING || selectedStructure.getFlag() == StructureFlag.FAILED) {
            throw new ResourceConflictException(
                    "Cannot select structure " + structureId + ": it is " + selectedStructure.getFlag()
                            + " and has no structure data available for selection.");
        }

        UUID reactantId = selectedStructure.getReactant().getId();

        // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): the sibling
        // ACTIVE/REJECTED reassignment below spans multiple ReactantStructure rows for this reactant, so a @Version
        // on any single row wouldn't stop two concurrent selectStructure calls (choosing different structures of
        // the same reactant) from each independently marking their own choice ACTIVE. Taking a pessimistic write
        // lock on the parent Reactant row here forces the second concurrent call to block until the first commits,
        // then re-read (via findByReactant_Id below) the now-up-to-date sibling flags before acting. Safe to hold:
        // the rest of this method is pure local DB reads/writes, no external I/O.
        reactantRepository.findByIdForUpdate(reactantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reactant not found with id: " + reactantId));

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
                .toList();
    }

    @Override
    @Scheduled(fixedDelay = PENDING_STRUCTURE_POLL_INTERVAL_MS)
    public void fetchPendingStructuresFromAPI() {
        List<ReactantStructure> pendingStructures = structureRepository.findByFlag(StructureFlag.PENDING);

        if (pendingStructures.isEmpty()) {
            return;
        }

        log.info("Found {} PENDING structure jobs. Polling external API...", pendingStructures.size());

        for (ReactantStructure pending : pendingStructures) {
            String jobId = pending.getExternalJobId();

            // FIXED (race condition inventory, see Software_todo.md's Race condition inventory item): this `pending`
            // row is held in memory across the slow checkJobStatus call below, then saved back further down - a wide
            // staleness window. ReactantStructure now carries @Version, so if a concurrent write (uploadStructure's
            // deleteAll, or selectStructure) removed or changed this exact row in between, the save() below throws
            // ObjectOptimisticLockingFailureException instead of silently resurrecting/overwriting stale data. The
            // surrounding catch(Exception) logs that as a per-job error without crashing the whole poll run, which is
            // the right behavior for a background poller - deliberately not narrowed to catch that exception
            // specifically, so it's handled the same as any other per-job failure.
            try {
                JobStatusResult result = structureApiClient.checkJobStatus(jobId);

                if (result.isFinished()) {
                    List<String> generatedStructures = result.structures();

                    if (generatedStructures != null && !generatedStructures.isEmpty()) {
                        log.info("Job {} succeeded. Received {} structures.", jobId, generatedStructures.size());

                        String primaryStructure = generatedStructures.getFirst();
                        if (isValidStructure(primaryStructure)) {
                            pending.setStructure(primaryStructure);
                            pending.setFlag(StructureFlag.READY);
                            structureRepository.save(pending);
                        } else {
                            log.warn("Job {} returned an invalid primary structure (null, blank, or longer than {} characters). Marking as FAILED.",
                                    jobId, MAX_STRUCTURE_LENGTH);
                            pending.setFlag(StructureFlag.FAILED);
                            structureRepository.save(pending);
                        }

                        for (int i = 1; i < generatedStructures.size(); i++) {
                            String candidateStructure = generatedStructures.get(i);
                            if (!isValidStructure(candidateStructure)) {
                                log.warn("Job {} returned an invalid structure at index {} (null, blank, or longer than {} characters). Skipping it.",
                                        jobId, i, MAX_STRUCTURE_LENGTH);
                                continue;
                            }
                            ReactantStructure additionalStructure = ReactantStructure.builder()
                                    .reactant(pending.getReactant())
                                    .externalJobId(jobId)
                                    .structure(candidateStructure)
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
                } else {
                    var status = result.status();
                    if (status != null && status.isPending()) {
                        log.debug("Job {} is still in progress (status: {}). Will check again on the next poll.", jobId, status);
                    } else {
                        log.warn("Job {} returned an unrecognized status: {}. Leaving it PENDING; will retry on the next poll.", jobId, status);
                    }
                }

            } catch (Exception e) {
                log.error("Failed to check status for pending Job ID: {}", jobId, e);
            }
        }
    }

    /**
     * Basic (deliberately non-chemistry-aware) content check for a structure string returned by
     * the external API: rejects {@code null}, blank/whitespace-only content, and anything beyond
     * {@link #MAX_STRUCTURE_LENGTH}. This does not attempt to parse or validate XYZ/MOL syntax.
     */
    private boolean isValidStructure(String structure) {
        return structure != null && !structure.isBlank() && structure.length() <= MAX_STRUCTURE_LENGTH;
    }
}
