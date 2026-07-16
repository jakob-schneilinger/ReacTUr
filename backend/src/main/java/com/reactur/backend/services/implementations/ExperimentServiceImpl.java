package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.exeptions.UnprocessableEntityException;
import com.reactur.backend.exeptions.ValidationException;
import com.reactur.backend.mappers.ExperimentMapper;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.ExperimentService;
import com.reactur.backend.services.FileService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reactur.backend.enums.Role;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import java.time.Instant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
  private final ExperimentMapper experimentMapper;
  private final ReactantStructureRepository structureRepository;
  private final FileService fileService;
  private final ExperimentAuthorization experimentAuthorization;

  @Override
  @Transactional
  public GetExperimentDto createExperiment(SaveExperimentDto experimentDto, String requestingUserId, Role requestingUserRole) {
    log.trace("Attempting to create new experiment with DOI: {} for user ID: {}", experimentDto.dois(), requestingUserId);

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

    validateReactantHasActiveStructure(reactantA, "A");
    validateReactantHasActiveStructure(reactantB, "B");

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
      .dois(experimentDto.dois())
      .reactantA(reactantA)
      .reactantB(reactantB)
      .metadata(metadata)
      .result(result)
      .createdBy(creator)
      .build();

    // ...saving the entities + the loggers

    Experiment savedExperiment = experimentRepository.save(experimentToSave);

    log.info("Successfully saved Experiment with internal ID: {}", savedExperiment.getId());

    // TODO: Should be moved to AuditService (later)

    // ...finally returning the saved element

    return experimentMapper.experimentToGetExperimentDto(savedExperiment, fileService.getAttachmentsOfExperiment(savedExperiment.getId()));
  }


  @Override
  public GetExperimentDto getExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole) {
    log.trace("Fetching experiment with ID: {}", experimentId);

    Experiment experiment = experimentRepository.findById(experimentId)
      .orElseThrow(() -> new ResourceNotFoundException(
        "Experiment with ID " + experimentId + " not found"
      ));

    experimentAuthorization.assertCanView(experiment, requestingUserId, requestingUserRole);

    return experimentMapper.experimentToGetExperimentDto(experiment, fileService.getAttachmentsOfExperiment(experiment.getId()));
  }


  @Override
  @Transactional
  public void archiveExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole) {
    log.trace("Attempting to archive experiment with ID: {}", experimentId);

    // 1. Fetch (Validation)
    Experiment experiment = experimentRepository.findById(experimentId)
      .orElseThrow(() -> new ResourceNotFoundException("Experiment with ID " + experimentId + " not found"));

    // 2. Authorize (Security)
    experimentAuthorization.assertCanModify(experiment, requestingUserId, requestingUserRole);

    // TODO: How should we do if the experiment was already archived? I just don't change anything but still
    //  return the 201 succeeded code.
    //  Maybe it would be useful to log.warn that a user is trying to archive something already archived...
    // 3. Act (Execution) - Only update if not already archived (Idempotent)
    if (experiment.getArchivedAt() == null) {
      experiment.setArchivedAt(Instant.now());
      experimentRepository.save(experiment);
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
    experimentAuthorization.assertCanModify(experiment, requestingUserId, requestingUserRole);

    // 3. Act
    if (experiment.getArchivedAt() != null) {
      experiment.setArchivedAt(null);
      experiment = experimentRepository.save(experiment);
    }

    return experimentMapper.experimentToGetExperimentDto(experiment, fileService.getAttachmentsOfExperiment(experiment.getId()));
  }

  @Override
  public GetExperimentDto[] getArchivedExperiments(String requestingUserId, Role requestingUserRole) {
    log.trace("Fetching archived experiments for user: {} with role: {}", requestingUserId, requestingUserRole);

    List<Experiment> archivedExperiments;

    if (experimentAuthorization.isPrivileged(requestingUserRole)) {
      log.debug("Privileged role detected. Fetching ALL archived experiments.");
      archivedExperiments = experimentRepository.findByArchivedAtIsNotNull();
    } else {
      log.debug("Standard role detected. Fetching archived experiments specifically for user.");
      archivedExperiments = experimentRepository.findArchivedExperimentsForUser(requestingUserId);
    }

    return archivedExperiments.stream()
            .map(exp -> experimentMapper.experimentToGetExperimentDto(
                    exp,
                    fileService.getAttachmentsOfExperiment(exp.getId())
            ))
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
    experimentAuthorization.assertCanModify(existingExperiment, requestingUserId, requestingUserRole);

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

    validateReactantHasActiveStructure(reactantA, "A");
    validateReactantHasActiveStructure(reactantB, "B");

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
    existingExperiment.setDois(experimentDto.dois());
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

    log.info("Successfully updated Experiment with internal ID: {}", savedExperiment.getId());

    return experimentMapper.experimentToGetExperimentDto(
            savedExperiment,
            fileService.getAttachmentsOfExperiment(savedExperiment.getId())
    );
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
    return experimentPage.map(exp -> experimentMapper.experimentToGetExperimentDto(
            exp,
            fileService.getAttachmentsOfExperiment(exp.getId())
    ));
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

    /**
     * Validates that a given Reactant has at least one associated ReactantStructure
     * with the flag set to ACTIVE via the repository.
     *
     * @param reactant The Reactant to validate.
     * @param label A label (e.g., "A" or "B") for clearer error messages.
     */
    private void validateReactantHasActiveStructure(Reactant reactant, String label) {
        List<ReactantStructure> structures = structureRepository.findByReactant_Id(reactant.getId());

        boolean hasActiveStructure = structures.stream()
                .anyMatch(structure -> structure.getFlag() == StructureFlag.ACTIVE);

        if (!hasActiveStructure) {
            throw new UnprocessableEntityException(
                    "Reactant " + label + " (ID: " + reactant.getId() + ") cannot be used. It does not have an ACTIVE 3D structure."
            );
        }
    }
}
