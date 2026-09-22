package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.enums.Role;
import com.reactur.backend.exceptions.ResourceNotFoundException;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.services.ExperimentHistoryService;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javers.core.Javers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.diff.changetype.ReferenceChange;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.object.InstanceId;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the human-readable audit trail for a single experiment, combining Javers' raw change log
 * for the experiment itself with the change logs of its {@link Metadata} and {@link
 * ExperimentResult} sub-entities into one chronological list.
 * <p>
 * Javers reports changes as a flat list of {@link Change} objects grouped only by commit. This
 * class is responsible for: (1) classifying each commit as CREATE/UPDATE/ARCHIVE/RESTORE, (2)
 * rendering each changed property as a "before -&gt; after" display string, and (3) resolving
 * reference IDs (instrument/modality/solvent/reactant/user UUIDs, and commit author IDs) to
 * human-readable names instead of raw UUIDs, batching those lookups instead of querying once per
 * individual change.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ExperimentHistoryServiceImpl implements ExperimentHistoryService {

    private final Javers javers;
    private final ExperimentRepository experimentRepository;
    private final InstrumentRepository instrumentRepository;
    private final ModalityRepository modalityRepository;
    private final SolventRepository solventRepository;
    private final ReactantRepository reactantRepository;
    private final UserRepository userRepository;
    private final ExperimentAuthorization experimentAuthorization;

    /**
     * Properties excluded from the rendered diff: {@code id} never changes, {@code metadata}/
     * {@code result} are references to sub-entities whose own field changes are already reported
     * as separate commits on those sub-entities (not useful to also show as a raw ID swap here),
     * and {@code archivedAt} is consumed separately by {@link #classifyCommitType} to derive the
     * ARCHIVE/RESTORE commit type, rather than shown as a plain property change.
     */
    private static final Set<String> IGNORED_PROPERTIES = Set.of("id", "metadata", "result", "archivedAt");

    @Override
    public List<ExperimentHistoryDto> getExperimentHistory(UUID experimentUuid, String requestingUserId, Role requestingUserRole) {
        Experiment experiment = experimentRepository.findById(experimentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        experimentAuthorization.assertCanView(experiment, requestingUserId, requestingUserRole);

        List<Change> allChanges = fetchAllChanges(experiment);
        Map<String, String> entityLookupMap = buildEntityLookupMap(allChanges);

        Map<CommitMetadata, List<Change>> changesByCommit = allChanges.stream()
                .filter(change -> change.getCommitMetadata().isPresent())
                .collect(Collectors.groupingBy(change -> change.getCommitMetadata().get()));

        return changesByCommit.entrySet().stream()
                .map(entry -> toHistoryDto(entry.getKey(), entry.getValue(), entityLookupMap))
                .sorted(Comparator.comparing(ExperimentHistoryDto::commitDate).reversed())
                .toList();
    }

    /**
     * Gathers every Javers change recorded against the experiment itself and its
     * Metadata/ExperimentResult sub-entities.
     *
     * @param experiment the experiment to fetch changes for
     * @return every change found, across all three entities
     */
    private List<Change> fetchAllChanges(Experiment experiment) {
        List<Change> allChanges = new ArrayList<>(javers.findChanges(
                QueryBuilder.byInstanceId(experiment.getId(), Experiment.class).build()
        ));

        if (experiment.getMetadata() != null) {
            allChanges.addAll(javers.findChanges(
                    QueryBuilder.byInstanceId(experiment.getMetadata().getId(), Metadata.class).build()
            ));
        }

        if (experiment.getResult() != null) {
            allChanges.addAll(javers.findChanges(
                    QueryBuilder.byInstanceId(experiment.getResult().getId(), ExperimentResult.class).build()
            ));
        }

        return allChanges;
    }

    /**
     * Assembles one {@link ExperimentHistoryDto} entry from a single Javers commit and its changes.
     *
     * @param commit           the commit's metadata (author, date)
     * @param changesInCommit  the changes recorded in this commit
     * @param entityLookupMap  ID-to-display-name lookups, from {@link #buildEntityLookupMap}
     * @return the assembled history entry
     */
    private ExperimentHistoryDto toHistoryDto(CommitMetadata commit, List<Change> changesInCommit, Map<String, String> entityLookupMap) {
        String commitType = classifyCommitType(changesInCommit);
        Map<String, String> diffMap = buildDiffMap(changesInCommit, entityLookupMap);
        String resolvedAuthor = entityLookupMap.getOrDefault(commit.getAuthor(), commit.getAuthor());

        return new ExperimentHistoryDto(resolvedAuthor, commit.getCommitDateInstant(), commitType, diffMap);
    }

    /**
     * A commit is CREATE if it contains Javers' {@link NewObject} marker, ARCHIVE/RESTORE if the
     * {@code archivedAt} property transitioned null&lt;-&gt;non-null, and UPDATE otherwise.
     *
     * @param changesInCommit the changes recorded in the commit
     * @return the classified commit type
     */
    private String classifyCommitType(List<Change> changesInCommit) {
        boolean isNewObject = changesInCommit.stream().anyMatch(NewObject.class::isInstance);
        if (isNewObject) {
            return "CREATE";
        }

        for (Change change : changesInCommit) {
            if (change instanceof ValueChange valueChange && "archivedAt".equals(propertyNameOf(change))) {
                if (valueChange.getLeft() == null && valueChange.getRight() != null) {
                    return "ARCHIVE";
                }
                if (valueChange.getLeft() != null && valueChange.getRight() == null) {
                    return "RESTORE";
                }
            }
        }

        return "UPDATE";
    }

    /**
     * Renders every non-ignored property change in a commit as a "before -> after" display string.
     *
     * @param changesInCommit the changes recorded in the commit
     * @param entityLookupMap ID-to-display-name lookups, from {@link #buildEntityLookupMap}
     * @return a map of property name to its rendered "before -> after" change
     */
    private Map<String, String> buildDiffMap(List<Change> changesInCommit, Map<String, String> entityLookupMap) {
        Map<String, String> diffMap = new HashMap<>();

        for (Change change : changesInCommit) {
            String propertyName = propertyNameOf(change);
            if (propertyName == null || IGNORED_PROPERTIES.contains(propertyName)) {
                continue;
            }

            String rendered = renderChange(change, entityLookupMap);
            if (rendered != null) {
                diffMap.put(propertyName, rendered);
            }
        }

        return diffMap;
    }

    /**
     * The property a change is about, or null if it's not a {@link PropertyChange} (e.g.
     * NewObject/ObjectRemoved).
     *
     * @param change the change to inspect
     * @return the property name, or null if the change has none
     */
    private String propertyNameOf(Change change) {
        return change instanceof PropertyChange<?> propertyChange ? propertyChange.getPropertyName() : null;
    }

    /**
     * Renders a single property change. {@link ReferenceChange} gets special handling to resolve
     * IDs to display names; every other {@link PropertyChange} (values, lists, sets, maps, and any
     * future Javers change type) is rendered generically via its own left/right values.
     * <p>
     * Matching against the shared {@link PropertyChange} contract, rather than enumerating each
     * concrete change type individually, means a property type this method doesn't specifically
     * know about (e.g. a list-typed change) still gets rendered instead of silently dropped from
     * the audit trail.
     *
     * @param change          the change to render
     * @param entityLookupMap ID-to-display-name lookups, from {@link #buildEntityLookupMap}
     * @return the rendered "before -> after" string, or null if the change carries no single
     *         property value of its own
     */
    private String renderChange(Change change, Map<String, String> entityLookupMap) {
        return switch (change) {
            case ReferenceChange refChange -> renderReferenceChange(refChange, entityLookupMap);
            case PropertyChange<?> propertyChange -> propertyChange.getLeft() + " -> " + propertyChange.getRight();
            default -> null; // NewObject/ObjectRemoved carry no single property value of their own.
        };
    }

    /**
     * Resolves both sides of a reference change from IDs to display names via {@code
     * entityLookupMap}, falling back to the raw ID string if not found.
     *
     * @param refChange       the reference change to render
     * @param entityLookupMap ID-to-display-name lookups, from {@link #buildEntityLookupMap}
     * @return the rendered "before -> after" string
     */
    private String renderReferenceChange(ReferenceChange refChange, Map<String, String> entityLookupMap) {
        Object leftId = idOf(refChange.getLeft());
        Object rightId = idOf(refChange.getRight());

        String leftDisplay = leftId != null ? entityLookupMap.getOrDefault(leftId.toString(), leftId.toString()) : "null";
        String rightDisplay = rightId != null ? entityLookupMap.getOrDefault(rightId.toString(), rightId.toString()) : "null";

        return leftDisplay + " -> " + rightDisplay;
    }

    /**
     * Extracts the raw concrete-domain-object ID (a UUID or a String, depending on the referenced
     * entity) from one side of a ReferenceChange.
     *
     * @param referenceEndpoint one side ({@code getLeft()}/{@code getRight()}) of a {@link ReferenceChange}
     * @return the raw ID, or null if the endpoint isn't a resolvable {@link InstanceId}
     */
    private Object idOf(Object referenceEndpoint) {
        return referenceEndpoint instanceof InstanceId instanceId ? instanceId.getCdoId() : null;
    }

    // --- Resolving reference IDs (instrument/modality/solvent/reactant/user) to display names ---

    /**
     * Groups the reference IDs collected from a batch of changes by which repository each one
     * belongs to.
     *
     * @param instrumentIds referenced instrument IDs
     * @param modalityIds   referenced modality IDs
     * @param solventIds    referenced solvent IDs
     * @param reactantIds   referenced reactant IDs
     * @param userIds       referenced user IDs (commit authors and scientist/creator references)
     */
    private record ReferencedIds(
            Set<UUID> instrumentIds,
            Set<UUID> modalityIds,
            Set<UUID> solventIds,
            Set<UUID> reactantIds,
            Set<String> userIds) {

        static ReferencedIds empty() {
            return new ReferencedIds(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
        }
    }

    /**
     * Batch-resolves every instrument/modality/solvent/reactant/user ID referenced anywhere in
     * {@code allChanges} to a human-readable display name, one query per repository rather than
     * one query per reference.
     *
     * @param allChanges every change collected for the experiment being rendered
     * @return a map of ID (as a string) to display name
     */
    private Map<String, String> buildEntityLookupMap(List<Change> allChanges) {
        ReferencedIds ids = collectReferencedIds(allChanges);

        Map<String, String> lookupMap = new HashMap<>();
        if (!ids.instrumentIds().isEmpty()) {
            instrumentRepository.findAllById(ids.instrumentIds()).forEach(i -> lookupMap.put(i.getId().toString(), i.getName()));
        }
        if (!ids.modalityIds().isEmpty()) {
            modalityRepository.findAllById(ids.modalityIds()).forEach(m -> lookupMap.put(m.getId().toString(), m.getName()));
        }
        if (!ids.solventIds().isEmpty()) {
            solventRepository.findAllById(ids.solventIds()).forEach(s -> lookupMap.put(s.getId().toString(), s.getName()));
        }
        if (!ids.reactantIds().isEmpty()) {
            reactantRepository.findAllById(ids.reactantIds()).forEach(r -> lookupMap.put(r.getId().toString(), r.getShortName()));
        }
        if (!ids.userIds().isEmpty()) {
            userRepository.findAllById(ids.userIds()).forEach(u -> lookupMap.put(u.getId(), u.getFirstName() + " " + u.getLastName()));
        }

        return lookupMap;
    }

    /**
     * Scans every change for the IDs that {@link #buildEntityLookupMap} will need to resolve:
     * every commit author, plus both sides of every {@link ReferenceChange}.
     *
     * @param allChanges every change collected for the experiment being rendered
     * @return the collected IDs, grouped by which repository each belongs to
     */
    private ReferencedIds collectReferencedIds(List<Change> allChanges) {
        ReferencedIds ids = ReferencedIds.empty();

        allChanges.stream()
                .map(Change::getCommitMetadata)
                .flatMap(Optional::stream)
                .map(CommitMetadata::getAuthor)
                .forEach(ids.userIds()::add);

        for (Change change : allChanges) {
            if (change instanceof ReferenceChange refChange) {
                addReferencedId(refChange.getPropertyName(), idOf(refChange.getLeft()), ids);
                addReferencedId(refChange.getPropertyName(), idOf(refChange.getRight()), ids);
            }
        }

        return ids;
    }

    /**
     * Adds {@code id} to the matching bucket in {@code ids} based on which reference property it
     * came from. Does nothing for a property this audit trail doesn't resolve a display name
     * for, or if the cast to the expected ID type fails.
     *
     * @param propertyName the name of the property the reference change is on
     * @param id           the raw referenced ID, or null
     * @param ids          the bucket set to add {@code id} to
     */
    private void addReferencedId(String propertyName, Object id, ReferencedIds ids) {
        if (id == null) {
            return;
        }
        try {
            switch (propertyName) {
                case "instrument" -> ids.instrumentIds().add((UUID) id);
                case "modality" -> ids.modalityIds().add((UUID) id);
                case "solvent" -> ids.solventIds().add((UUID) id);
                case "reactantA", "reactantB" -> ids.reactantIds().add((UUID) id);
                case "scientist", "createdBy" -> ids.userIds().add((String) id);
                default -> { } // Not a reference property this audit trail resolves a display name for.
            }
        } catch (ClassCastException e) {
            log.warn("JaVers reference ID for property {} could not be cast", propertyName, e);
        }
    }
}
