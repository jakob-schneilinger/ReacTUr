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
 * human-readable names instead of raw UUIDs - batching those lookups instead of querying once per
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
        log.trace("Fetching history for experiment ID: {}", experimentUuid);

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

    /** Gathers every Javers change recorded against the experiment itself and its Metadata/ExperimentResult sub-entities. */
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

    private ExperimentHistoryDto toHistoryDto(CommitMetadata commit, List<Change> changesInCommit, Map<String, String> entityLookupMap) {
        String commitType = classifyCommitType(changesInCommit);
        Map<String, String> diffMap = buildDiffMap(changesInCommit, entityLookupMap);
        String resolvedAuthor = entityLookupMap.getOrDefault(commit.getAuthor(), commit.getAuthor());

        return new ExperimentHistoryDto(resolvedAuthor, commit.getCommitDateInstant(), commitType, diffMap);
    }

    /**
     * A commit is CREATE if it contains Javers' {@link NewObject} marker, ARCHIVE/RESTORE if the
     * {@code archivedAt} property transitioned null&lt;-&gt;non-null, and UPDATE otherwise.
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

    /** Renders every non-ignored property change in a commit as a "before -> after" display string. */
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

    private String propertyNameOf(Change change) {
        return change instanceof PropertyChange<?> propertyChange ? propertyChange.getPropertyName() : null;
    }

    /**
     * Renders a single property change. {@link ReferenceChange} gets special handling to resolve
     * IDs to display names; every other {@link PropertyChange} - values, lists, sets, maps, and
     * any future Javers change type - is rendered generically via its own left/right values.
     * <p>
     * This is deliberate: the previous version of this class enumerated ValueChange/ReferenceChange
     * explicitly and silently dropped list-typed property changes (e.g. {@code Experiment.dois})
     * from the audit trail entirely, because Javers' {@code ListChange} matched neither case -
     * confirmed with a real Javers instance in a throwaway test before this rewrite. Falling back
     * to the shared {@link PropertyChange} contract instead of an exhaustive type list means no
     * future property type (list, set, map, or otherwise) can go silently unhandled again.
     */
    private String renderChange(Change change, Map<String, String> entityLookupMap) {
        return switch (change) {
            case ReferenceChange refChange -> renderReferenceChange(refChange, entityLookupMap);
            case PropertyChange<?> propertyChange -> propertyChange.getLeft() + " -> " + propertyChange.getRight();
            default -> null; // NewObject/ObjectRemoved carry no single property value of their own.
        };
    }

    private String renderReferenceChange(ReferenceChange refChange, Map<String, String> entityLookupMap) {
        Object leftId = idOf(refChange.getLeft());
        Object rightId = idOf(refChange.getRight());

        String leftDisplay = leftId != null ? entityLookupMap.getOrDefault(leftId.toString(), leftId.toString()) : "null";
        String rightDisplay = rightId != null ? entityLookupMap.getOrDefault(rightId.toString(), rightId.toString()) : "null";

        return leftDisplay + " -> " + rightDisplay;
    }

    /** Extracts the raw concrete-domain-object ID (a UUID or a String, depending on the referenced entity) from one side of a ReferenceChange. */
    private Object idOf(Object referenceEndpoint) {
        return referenceEndpoint instanceof InstanceId instanceId ? instanceId.getCdoId() : null;
    }

    // --- Resolving reference IDs (instrument/modality/solvent/reactant/user) to display names ---

    /** Groups the reference IDs collected from a batch of changes by which repository each one belongs to. */
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
