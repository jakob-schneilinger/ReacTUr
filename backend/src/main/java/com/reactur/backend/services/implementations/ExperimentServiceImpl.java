package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.entities.AuditLog;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.AuditAction;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.exeptions.UnprocessableEntityException;
import com.reactur.backend.exeptions.ValidationException;
import com.reactur.backend.mappers.ExperimentMapper;
import com.reactur.backend.repositories.AuditLogRepository;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.ExperimentService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javers.core.Javers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.metamodel.object.InstanceId;
import org.javers.repository.jql.QueryBuilder;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reactur.backend.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ExperimentServiceImpl implements ExperimentService {

  private final ExperimentRepository experimentRepository;
  private final ReactantRepository reactantRepository;
  private final SolventRepository solventRepository;
  private final InstrumentRepository instrumentRepository;
  private final ModalityRepository modalityRepository;
  private final UserRepository userRepository;
  private final AuditLogRepository auditLogRepository;
  private final ExperimentMapper experimentMapper;
  private final Javers javers;

  @Override
  @Transactional
  public GetExperimentDto createExperiment(SaveExperimentDto experimentDto, String requestingUserId, Role requestingUserRole) {
    log.trace("Attempting to create new experiment with DOI: {} for user ID: {}", experimentDto.doi(), requestingUserId);

    // First, semantic Validation

    // Who physically clicks 'Save': (Extracted from Keycloak Token)
    User creator = userRepository.findById(requestingUserId)
      .orElseThrow(() -> {
        log.error(
          "CRITICAL: Authenticated Keycloak user {} not found in internal database. " +
            "UserSyncFilter may have failed.", requestingUserId
        );
        return new IllegalStateException("System state error: Authenticated user lacks internal profile.");
      });

    // Who actually performed the experiment: (Extracted from DTO payload)
    User scientist = userRepository.findById(experimentDto.metadata().scientist())
      .orElseThrow(() -> new UnprocessableEntityException(
        "Scientist with ID " + experimentDto.metadata().scientist() + " not found."
      ));

    // TODO: Move to Validator? We can discuss Later

    Reactant reactantA = reactantRepository.findById(experimentDto.reactantA())
      .orElseThrow(() -> new UnprocessableEntityException("Reactant A does not exist."));

    Reactant reactantB = reactantRepository.findById(experimentDto.reactantB())
      .orElseThrow(() -> new UnprocessableEntityException("Reactant B does not exist."));

    if (reactantA.getId().equals(reactantB.getId())) {
      throw new ValidationException("Reactants A and B cannot be the same.");
    }

    Solvent solvent = solventRepository.findById(experimentDto.metadata().solvent())
      .orElseThrow(() -> new UnprocessableEntityException("Solvent does not exist."));

    Instrument instrument = null;
    if (experimentDto.metadata().instrument() != null) {
      instrument = instrumentRepository.findById(experimentDto.metadata().instrument())
        .orElseThrow(() -> new UnprocessableEntityException("Instrument does not exist."));
    }

    Modality modality = null;
    if (experimentDto.metadata().modality() != null) {
      modality = modalityRepository.findById(experimentDto.metadata().modality())
        .orElseThrow(() -> new UnprocessableEntityException("Modality does not exist."));
    }

    log.debug("All chemical and lookup entities successfully validated and fetched.");

    // Then creating the entities...

    Metadata metadata = Metadata.builder()
      .date(experimentDto.metadata().date())
      .scientist(scientist)
      .temperature(experimentDto.metadata().temperature())
      .solvent(solvent)
      .concentrationA(experimentDto.metadata().concentrationA())
      .concentrationB(experimentDto.metadata().concentrationB())
      .instrument(instrument)
      .modality(modality)
      .wavelength(experimentDto.metadata().wavelength())
      .comment(experimentDto.metadata().comment())
      .build();

    ExperimentResult result = ExperimentResult.builder()
      .rateConstant(experimentDto.measurementData().rateConstant())
      .pfoRateConstant(experimentDto.measurementData().pFO_rateConstant())
      .errorValue(experimentDto.measurementData().error())
      .isErrorPercentage(experimentDto.measurementData().isErrorPercentage())
      .build();

    Experiment experimentToSave = Experiment.builder()
      .doi(experimentDto.doi())
      .reactantA(reactantA)
      .reactantB(reactantB)
      .metadata(metadata)
      .result(result)
      .createdBy(creator)
      .build();

    // ... aand saving the entities + the loggers

    Experiment savedExperiment = experimentRepository.save(experimentToSave);

    log.info("Successfully saved Experiment with internal ID: {}", savedExperiment.getId());

    // TODO: Should be moved to AuditService (later)

    AuditLog auditLog = AuditLog.builder()
      .experimentId(savedExperiment.getId())
      .userId(requestingUserId)
      .action(AuditAction.CREATE)
      // TODO: Decide how to serialize the JSON diffs for changeFrom/changeTo
      .build();
    auditLogRepository.save(auditLog);

    // ...finally returning the saved element

    return experimentMapper.experimentToGetExperimentDto(savedExperiment);
  }


  @Override
  public GetExperimentDto getExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole) {
    log.trace("Fetching experiment with ID: {}", experimentId);

    Experiment experiment = experimentRepository.findById(experimentId)
      .orElseThrow(() -> new ResourceNotFoundException(
        "Experiment with ID " + experimentId + " not found"
      ));

    // Security Check for Archived Data
    if (experiment.getArchivedAt() != null) {
      boolean isCreator = experiment.getCreatedBy().getId().equals(requestingUserId);
      boolean isScientist = experiment.getMetadata().getScientist().getId().equals(requestingUserId);

      if (requestingUserRole != Role.ADMIN && !isCreator && !isScientist) {
        log.warn("Security blocked access to archived experiment {} for user {}", experimentId, requestingUserId);
        throw new AccessDeniedException("You do not have permission to view this archived experiment.");
      }
    }

    return experimentMapper.experimentToGetExperimentDto(experiment);
  }


  @Override
  @Transactional
  public void archiveExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole) {
    log.trace("Attempting to archive experiment with ID: {}", experimentId);

    // 1. Fetch (Validation)
    Experiment experiment = experimentRepository.findById(experimentId)
      .orElseThrow(() -> new ResourceNotFoundException("Experiment with ID " + experimentId + " not found"));

    // 2. Authorize (Security)
    boolean isCreator = experiment.getCreatedBy().getId().equals(requestingUserId);
    boolean isScientist = experiment.getMetadata().getScientist().getId().equals(requestingUserId);

    if (requestingUserRole != Role.ADMIN && !isCreator && !isScientist) {
      log.warn("Security blocked archive attempt on experiment {} by user {}", experimentId, requestingUserId);
      throw new AccessDeniedException("You do not have permission to archive this experiment.");
    }

    // TODO: How should we do if the experiment was already archived? I just don't change anything but still
    //  return the 201 succeeded code.
    //  Maybe it would be useful to log.warn that a user is trying to archive something already archived...
    // 3. Act (Execution) - Only update if not already archived (Idempotent)
    if (experiment.getArchivedAt() == null) {
      experiment.setArchivedAt(Instant.now());
      experimentRepository.save(experiment);

      AuditLog auditLog = AuditLog.builder()
        .experimentId(experimentId)
        .userId(requestingUserId)
        .action(AuditAction.UPDATE) // TODO: Change to ARCHIVE or something in the enum!
        .build();
      auditLogRepository.save(auditLog);
    }
  }


  @Override
  @Transactional
  public GetExperimentDto restoreExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole) {
    log.trace("Attempting to restore experiment with ID: {}", experimentId);

    // 1. Fetch
    Experiment experiment = experimentRepository.findById(experimentId)
      .orElseThrow(() -> new ResourceNotFoundException("Experiment with ID " + experimentId + " not found"));

    // 2. Authorize
    boolean isCreator = experiment.getCreatedBy().getId().equals(requestingUserId);
    boolean isScientist = experiment.getMetadata().getScientist().getId().equals(requestingUserId);

    if (requestingUserRole != Role.ADMIN && !isCreator && !isScientist) {
      log.warn("Security blocked restore attempt on experiment {} by user {}", experimentId, requestingUserId);
      throw new AccessDeniedException("You do not have permission to restore this experiment.");
    }

    // 3. Act
    if (experiment.getArchivedAt() != null) {
      experiment.setArchivedAt(null);
      experiment = experimentRepository.save(experiment);

      AuditLog auditLog = AuditLog.builder()
        .experimentId(experimentId)
        .userId(requestingUserId)
        .action(AuditAction.UPDATE) // TODO: Change to RESTORE if added to the enum
        .build();
      auditLogRepository.save(auditLog);
    }

    return experimentMapper.experimentToGetExperimentDto(experiment);
  }

  @Override
  public GetExperimentDto[] getArchivedExperiments(String requestingUserId, Role requestingUserRole) {
    log.trace("Fetching archived experiments for user: {} with role: {}", requestingUserId, requestingUserRole);

    List<Experiment> archivedExperiments;

    if (requestingUserRole == Role.ADMIN) {
      log.debug("Admin role detected. Fetching ALL archived experiments.");
      archivedExperiments = experimentRepository.findByArchivedAtIsNotNull();
    } else {
      log.debug("Standard role detected. Fetching archived experiments specifically for user.");
      archivedExperiments = experimentRepository.findArchivedExperimentsForUser(requestingUserId);
    }

    return archivedExperiments.stream()
      .map(experimentMapper::experimentToGetExperimentDto)
      .toArray(GetExperimentDto[]::new);
  }

  @Override
  @Transactional
  public GetExperimentDto updateExperiment(
    UUID experimentId,
    SaveExperimentDto experimentDto,
    String requestingUserId,
    Role requestingUserRole
  ) {
    log.trace("Attempting to update experiment with ID: {}", experimentId);

    // 1. Fetch Existing (Validation)
    Experiment existingExperiment = experimentRepository.findById(experimentId)
      .orElseThrow(() -> new ResourceNotFoundException("Experiment with ID " + experimentId + " not found"));

    // 2. Check Immutable State (Archived experiments are frozen)
    if (existingExperiment.getArchivedAt() != null) {
      log.warn("Attempted to update archived experiment {} by user {}", experimentId, requestingUserId);
      throw new ResourceConflictException("Cannot update an archived experiment. Please restore it first.");
    }

    // 3. Authorize (Security)
    boolean isCreator = existingExperiment.getCreatedBy().getId().equals(requestingUserId);
    boolean isScientist = existingExperiment.getMetadata().getScientist().getId().equals(requestingUserId);

    if (requestingUserRole != Role.ADMIN && !isCreator && !isScientist) {
      log.warn("Security blocked update attempt on experiment {} by user {}", experimentId, requestingUserId);
      throw new AccessDeniedException("You do not have permission to update this experiment.");
    }

    // 4. Semantic Validation (Lookups for new DTO data)
    User scientist = userRepository.findById(experimentDto.metadata().scientist())
      .orElseThrow(() -> new UnprocessableEntityException("Scientist with ID " + experimentDto.metadata().scientist() + " not found."));

    Reactant reactantA = reactantRepository.findById(experimentDto.reactantA())
      .orElseThrow(() -> new UnprocessableEntityException("Reactant A does not exist."));

    Reactant reactantB = reactantRepository.findById(experimentDto.reactantB())
      .orElseThrow(() -> new UnprocessableEntityException("Reactant B does not exist."));

    if (reactantA.getId().equals(reactantB.getId())) {
      log.warn("Reactant A and B cannot be the same in an update.");
      throw new ValidationException("Reactant A and B cannot be the same in an update.");
    }

    Solvent solvent = solventRepository.findById(experimentDto.metadata().solvent())
      .orElseThrow(() -> new UnprocessableEntityException("Solvent does not exist."));

    Instrument instrument = null;
    if (experimentDto.metadata().instrument() != null) {
      instrument = instrumentRepository.findById(experimentDto.metadata().instrument())
        .orElseThrow(() -> new UnprocessableEntityException("Instrument does not exist."));
    }

    Modality modality = null;
    if (experimentDto.metadata().modality() != null) {
      modality = modalityRepository.findById(experimentDto.metadata().modality())
        .orElseThrow(() -> new UnprocessableEntityException("Modality does not exist."));
    }

    log.debug("All chemical and lookup entities successfully validated for update.");

    // 5. Apply Updates (Full Replacement via PUT)
    // Note: We intentionally DO NOT update `createdBy` or `archivedAt` to enforce immutability.
    existingExperiment.setDoi(experimentDto.doi());
    existingExperiment.setReactantA(reactantA);
    existingExperiment.setReactantB(reactantB);

    Metadata metadata = existingExperiment.getMetadata();
    metadata.setDate(experimentDto.metadata().date());
    metadata.setScientist(scientist);
    metadata.setTemperature(experimentDto.metadata().temperature());
    metadata.setSolvent(solvent);
    metadata.setConcentrationA(experimentDto.metadata().concentrationA());
    metadata.setConcentrationB(experimentDto.metadata().concentrationB());
    metadata.setInstrument(instrument);
    metadata.setModality(modality);
    metadata.setWavelength(experimentDto.metadata().wavelength());
    metadata.setComment(experimentDto.metadata().comment());

    ExperimentResult result = existingExperiment.getResult();
    result.setRateConstant(experimentDto.measurementData().rateConstant());
    result.setPfoRateConstant(experimentDto.measurementData().pFO_rateConstant());
    result.setErrorValue(experimentDto.measurementData().error());
    result.setIsErrorPercentage(experimentDto.measurementData().isErrorPercentage());

    // 6. Save & Audit
    Experiment savedExperiment = experimentRepository.save(existingExperiment);

    AuditLog auditLog = AuditLog.builder()
      .experimentId(savedExperiment.getId())
      .userId(requestingUserId)
      .action(AuditAction.UPDATE)
      // TODO: Generate JSON diffs for changeFrom/changeTo
      .build();
    auditLogRepository.save(auditLog);

    log.info("Successfully updated Experiment with internal ID: {}", savedExperiment.getId());

    return experimentMapper.experimentToGetExperimentDto(savedExperiment);
  }

  @Override
  public Page<GetExperimentDto> searchExperiments(
    SearchParametersDto searchParams,
    Pageable pageable,
    String requestingUserId,
    Role requestingUserRole
  ) {
    log.trace("Executing dynamic search for experiments by user: {}", requestingUserId);

    // Dynamic SQL WHERE clause
    Specification<Experiment> spec = buildSearchSpecification(searchParams);

    // Execute the paginated query
    Page<Experiment> experimentPage = experimentRepository.findAll(spec, pageable);

    // Map entities to DTOs while preserving the Page structure
    return experimentPage.map(experimentMapper::experimentToGetExperimentDto);
  }

  // Dynamic Query Builder
  private Specification<Experiment> buildSearchSpecification(SearchParametersDto params) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // GLOBAL RULE: Never show archived experiments in active search
      predicates.add(cb.isNull(root.get("archivedAt")));

      if (params == null) {
        return cb.and(predicates.toArray(new Predicate[0]));
      }

      // Reactant commutativity rule
      boolean hasA = params.reactantA() != null && !params.reactantA().isEmpty();
      boolean hasB = params.reactantB() != null && !params.reactantB().isEmpty();

      if (hasA && hasB) {
        // (A in listA AND B in listB) OR (A in listB AND B in listA)
        Predicate directMatch = cb.and(
          root.get("reactantA").get("id").in(params.reactantA()),
          root.get("reactantB").get("id").in(params.reactantB())
        );
        Predicate crossMatch = cb.and(
          root.get("reactantA").get("id").in(params.reactantB()),
          root.get("reactantB").get("id").in(params.reactantA())
        );
        predicates.add(cb.or(directMatch, crossMatch));
      } else if (hasA) {
        // Look for ListA in EITHER Reactant A or Reactant B column
        predicates.add(cb.or(
          root.get("reactantA").get("id").in(params.reactantA()),
          root.get("reactantB").get("id").in(params.reactantA())
        ));
      } else if (hasB) {
        // Look for ListB in EITHER Reactant A or Reactant B column
        predicates.add(cb.or(
          root.get("reactantA").get("id").in(params.reactantB()),
          root.get("reactantB").get("id").in(params.reactantB())
        ));
      }

      // METADATA JOINS (We only join if we need to filter on metadata fields)
      Join<Experiment, Metadata> metadataJoin = root.join("metadata");

      if (params.dateMin() != null) predicates.add(cb.greaterThanOrEqualTo(metadataJoin.get("date"), params.dateMin()));
      if (params.dateMax() != null) predicates.add(cb.lessThanOrEqualTo(metadataJoin.get("date"), params.dateMax()));

      if (params.temperatureMin() != null) predicates.add(cb.greaterThanOrEqualTo(metadataJoin.get("temperature"), params.temperatureMin()));
      if (params.temperatureMax() != null) predicates.add(cb.lessThanOrEqualTo(metadataJoin.get("temperature"), params.temperatureMax()));

      if (params.concentrationA_Min() != null) predicates.add(cb.greaterThanOrEqualTo(metadataJoin.get("concentrationA"), params.concentrationA_Min()));
      if (params.concentrationA_Max() != null) predicates.add(cb.lessThanOrEqualTo(metadataJoin.get("concentrationA"), params.concentrationA_Max()));

      if (params.concentrationB_Min() != null) predicates.add(cb.greaterThanOrEqualTo(metadataJoin.get("concentrationB"), params.concentrationB_Min()));
      if (params.concentrationB_Max() != null) predicates.add(cb.lessThanOrEqualTo(metadataJoin.get("concentrationB"), params.concentrationB_Max()));

      if (params.wavelengthMin() != null) predicates.add(cb.greaterThanOrEqualTo(metadataJoin.get("wavelength"), params.wavelengthMin()));
      if (params.wavelengthMax() != null) predicates.add(cb.lessThanOrEqualTo(metadataJoin.get("wavelength"), params.wavelengthMax()));

      if (params.comment() != null && !params.comment().isBlank()) {
        // case-insensitive search
        predicates.add(cb.like(cb.lower(metadataJoin.get("comment")), "%" + params.comment().toLowerCase() + "%"));
      }

      // METADATA LISTS (OR Logic via IN clauses)
      if (params.scientist() != null && !params.scientist().isEmpty()) {
        predicates.add(metadataJoin.get("scientist").get("id").in(params.scientist()));
      }
      if (params.solvent() != null) {
        predicates.add(cb.equal(metadataJoin.get("solvent").get("id"), params.solvent()));
      }
      if (params.instrument() != null) {
        predicates.add(cb.equal(metadataJoin.get("instrument").get("id"), params.instrument()));
      }
      if (params.modality() != null) {
        predicates.add(cb.equal(metadataJoin.get("modality").get("id"), params.modality()));
      }

      // RESULT JOINS (For MeasurementSearchDto)
      if (params.measurementSearch() != null) {
        jakarta.persistence.criteria.Join<Experiment, ExperimentResult> resultJoin = root.join("result");
        var mSearch = params.measurementSearch();

        if (mSearch.rateConstantMin() != null) predicates.add(cb.greaterThanOrEqualTo(resultJoin.get("rateConstant"), mSearch.rateConstantMin()));
        if (mSearch.rateConstantMax() != null) predicates.add(cb.lessThanOrEqualTo(resultJoin.get("rateConstant"), mSearch.rateConstantMax()));

        if (mSearch.pFO_rateConstantMin() != null) predicates.add(cb.greaterThanOrEqualTo(resultJoin.get("pfoRateConstant"), mSearch.pFO_rateConstantMin()));
        if (mSearch.pFO_rateConstantMax() != null) predicates.add(cb.lessThanOrEqualTo(resultJoin.get("pfoRateConstant"), mSearch.pFO_rateConstantMax()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  public List<ExperimentHistoryDto> getExperimentHistory(UUID experimentUuid) {
    Experiment experiment = experimentRepository.findById(experimentUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

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

    Map<CommitMetadata, List<Change>> changesByCommit = allChanges.stream()
            .filter(change -> change.getCommitMetadata().isPresent())
            .collect(Collectors.groupingBy(change -> change.getCommitMetadata().get()));

    List<ExperimentHistoryDto> historyList = getExperimentHistoryDtos(changesByCommit);

    historyList.sort((a, b) -> b.commitDate().compareTo(a.commitDate()));

    return historyList;
  }

  private static @NonNull List<ExperimentHistoryDto> getExperimentHistoryDtos(Map<CommitMetadata, List<Change>> changesByCommit) {
    List<ExperimentHistoryDto> historyList = new ArrayList<>();

    for (Map.Entry<CommitMetadata, List<Change>> entry : changesByCommit.entrySet()) {
      CommitMetadata commit = entry.getKey();
      Map<String, String> diffMap = new HashMap<>();

      String commitType = "UPDATE";

      for (Change change : entry.getValue()) {

        if (change instanceof org.javers.core.diff.changetype.NewObject) {
          commitType = "CREATE";
        }

        if (change instanceof org.javers.core.diff.changetype.ValueChange valChange) {
          String propName = valChange.getPropertyName();

          if ("id".equals(propName) && valChange.getLeft() == null) {
            commitType = "CREATE";
          }

          if ("archivedAt".equals(propName)) {
            if (valChange.getLeft() == null && valChange.getRight() != null) {
              commitType = "ARCHIVE";
            } else if (valChange.getLeft() != null && valChange.getRight() == null) {
              commitType = "RESTORE";
            }
          }

          diffMap.put(
                  propName,
                  valChange.getLeft() + " -> " + valChange.getRight()
          );
        } else if (change instanceof org.javers.core.diff.changetype.ReferenceChange refChange) {
          String propName = refChange.getPropertyName();

          Object leftId = refChange.getLeft() instanceof InstanceId instanceId ? instanceId.getCdoId() : null;
          Object rightId = refChange.getRight() instanceof InstanceId instanceId ? instanceId.getCdoId() : null;

          diffMap.put(
                  propName,
                  (leftId != null ? leftId : "null") + " -> " + (rightId != null ? rightId : "null")
          );
        }
      }

      historyList.add(new ExperimentHistoryDto(
              commit.getAuthor(),
              commit.getCommitDateInstant(),
              commitType,
              diffMap
      ));
    }
    return historyList;
  }

}
