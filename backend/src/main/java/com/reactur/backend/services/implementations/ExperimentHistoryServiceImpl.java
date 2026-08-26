package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.enums.Role;
import com.reactur.backend.services.ExperimentHistoryService;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import lombok.NonNull;
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
import com.reactur.backend.entities.experiment.Metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private static final Set<String> IGNORED_PROPERTIES = Set.of(
            "id", "metadata", "result", "archivedAt"
    );

    @Override
    public List<ExperimentHistoryDto> getExperimentHistory(UUID experimentUuid, String requestingUserId, Role requestingUserRole) {
        log.trace("Fetching history for experiment ID: {}", experimentUuid);

        Experiment experiment = experimentRepository.findById(experimentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        experimentAuthorization.assertCanView(experiment, requestingUserId, requestingUserRole);

        List<Change> allChanges = new ArrayList<>(javers.findChanges(
                QueryBuilder.byInstanceId(experimentUuid, Experiment.class).build()
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

        Map<String, String> entityLookupMap = buildEntityLookupMap(allChanges);

        Map<CommitMetadata, List<Change>> changesByCommit = allChanges.stream()
                .filter(change -> change.getCommitMetadata().isPresent())
                .collect(Collectors.groupingBy(change -> change.getCommitMetadata().get()));

        List<ExperimentHistoryDto> historyList = getExperimentHistoryDtos(changesByCommit, entityLookupMap);

        historyList.sort((a, b) -> b.commitDate().compareTo(a.commitDate()));

        return historyList;
    }

    private Map<String, String> buildEntityLookupMap(List<Change> allChanges) {
        Set<UUID> instrumentIds = new HashSet<>();
        Set<UUID> modalityIds = new HashSet<>();
        Set<UUID> solventIds = new HashSet<>();
        Set<UUID> reactantIds = new HashSet<>();
        Set<String> userIds = new HashSet<>();

        allChanges.stream()
                .filter(change -> change.getCommitMetadata().isPresent())
                .map(change -> change.getCommitMetadata().get().getAuthor())
                .forEach(userIds::add);

        for (Change change : allChanges) {
            if (change instanceof ReferenceChange refChange) {
                String propName = refChange.getPropertyName();

                Object leftId = refChange.getLeft() instanceof InstanceId id ? id.getCdoId() : null;
                Object rightId = refChange.getRight() instanceof InstanceId id ? id.getCdoId() : null;

                extractId(propName, leftId, instrumentIds, modalityIds, solventIds, reactantIds, userIds);
                extractId(propName, rightId, instrumentIds, modalityIds, solventIds, reactantIds, userIds);
            }
        }

        Map<String, String> lookupMap = new HashMap<>();

        if (!instrumentIds.isEmpty()) instrumentRepository.findAllById(instrumentIds).forEach(i -> lookupMap.put(i.getId().toString(), i.getName()));
        if (!modalityIds.isEmpty()) modalityRepository.findAllById(modalityIds).forEach(m -> lookupMap.put(m.getId().toString(), m.getName()));
        if (!solventIds.isEmpty()) solventRepository.findAllById(solventIds).forEach(s -> lookupMap.put(s.getId().toString(), s.getName()));
        if (!reactantIds.isEmpty()) reactantRepository.findAllById(reactantIds).forEach(r -> lookupMap.put(r.getId().toString(), r.getShortName()));
        if (!userIds.isEmpty()) userRepository.findAllById(userIds).forEach(u -> lookupMap.put(u.getId(), u.getFirstName() + " " + u.getLastName()));

        return lookupMap;
    }

    private void extractId(String propName, Object idObj, Set<UUID> instrumentIds, Set<UUID> modalityIds,
                           Set<UUID> solventIds, Set<UUID> reactantIds, Set<String> userIds) {
        if (idObj == null) return;
        try {
            switch (propName) {
                case "instrument" -> instrumentIds.add((UUID) idObj);
                case "modality" -> modalityIds.add((UUID) idObj);
                case "solvent" -> solventIds.add((UUID) idObj);
                case "reactantA", "reactantB" -> reactantIds.add((UUID) idObj);
                case "scientist", "createdBy" -> userIds.add((String) idObj);
            }
        } catch (ClassCastException e) {
            log.warn("JaVers reference ID for property {} could not be cast", propName, e);
        }
    }

    private @NonNull List<ExperimentHistoryDto> getExperimentHistoryDtos(
            Map<CommitMetadata, List<Change>> changesByCommit,
            Map<String, String> entityLookupMap) {

        List<ExperimentHistoryDto> historyList = new ArrayList<>();

        for (Map.Entry<CommitMetadata, List<Change>> entry : changesByCommit.entrySet()) {
            CommitMetadata commit = entry.getKey();
            Map<String, String> diffMap = new HashMap<>();
            String commitType = "UPDATE";

            for (Change change : entry.getValue()) {
                if (change instanceof NewObject) {
                    commitType = "CREATE";
                }

                String propName = "";
                if (change instanceof PropertyChange propChange) {
                    propName = propChange.getPropertyName();
                }

                if (IGNORED_PROPERTIES.contains(propName)) {
                    // Only need to check for archivedAt here, to set the status to ARCHIVE/RESTORE.
                    if ("archivedAt".equals(propName) && change instanceof ValueChange valChange) {
                        if (valChange.getLeft() == null && valChange.getRight() != null) commitType = "ARCHIVE";
                        else if (valChange.getLeft() != null && valChange.getRight() == null) commitType = "RESTORE";
                    }
                    continue; // Skips straight to the next change, without writing this property into diffMap.
                }

                // Not an ignored property - map it normally:
                if (change instanceof ValueChange valChange) {
                    diffMap.put(propName, valChange.getLeft() + " -> " + valChange.getRight());

                } else if (change instanceof ReferenceChange refChange) {
                    Object leftIdObj = refChange.getLeft() instanceof InstanceId instanceId ? instanceId.getCdoId() : null;
                    Object rightIdObj = refChange.getRight() instanceof InstanceId instanceId ? instanceId.getCdoId() : null;

                    String leftId = leftIdObj != null ? leftIdObj.toString() : null;
                    String rightId = rightIdObj != null ? rightIdObj.toString() : null;

                    String leftDisplay = leftId != null ? entityLookupMap.getOrDefault(leftId, leftId) : "null";
                    String rightDisplay = rightId != null ? entityLookupMap.getOrDefault(rightId, rightId) : "null";

                    diffMap.put(propName, leftDisplay + " -> " + rightDisplay);
                }
            }

            // Resolve the author's display name via the lookup map, falling back to the raw author ID
            // (e.g. a Keycloak subject UUID) if no matching User was found.
            String resolvedAuthor = entityLookupMap.getOrDefault(commit.getAuthor(), commit.getAuthor());

            historyList.add(new ExperimentHistoryDto(
                    resolvedAuthor,
                    commit.getCommitDateInstant(),
                    commitType,
                    diffMap
            ));
        }
        return historyList;
    }
}
