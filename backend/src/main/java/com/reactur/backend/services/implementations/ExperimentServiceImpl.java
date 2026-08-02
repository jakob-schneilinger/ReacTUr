package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.entities.User;
import com.reactur.backend.exeptions.ResourceConflictException;
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.mappers.ExperimentMapper;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.ExperimentService;
import com.reactur.backend.services.FileService;
import com.reactur.backend.validators.ExperimentValidator;
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
  private final UserRepository userRepository;
  private final ExperimentMapper experimentMapper;
  private final FileService fileService;
  private final ExperimentAuthorization experimentAuthorization;
  private final ExperimentValidator experimentValidator;

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
            "CustomJwtAuthenticationConverter's user-sync may have failed.", requestingUserId
        );
        return new IllegalStateException("System state error: Authenticated user lacks internal profile.");
      });

    // Who actually performed the experiment, plus every other referenced entity (reactants,
    // solvent, instrument, modality): validated and resolved via ExperimentValidator, which
    // returns them so we don't have to re-fetch them here.
    ExperimentValidator.ValidatedReferences validated = experimentValidator.validateForCreate(experimentDto);
    User scientist = validated.scientist();
    Reactant reactantA = validated.reactantA();
    Reactant reactantB = validated.reactantB();
    Solvent solvent = validated.solvent();
    Instrument instrument = validated.instrument();
    Modality modality = validated.modality();

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

    // 2. Authorize (Security) - runs before state checks below, so that an unauthorized
    // caller always gets a 403 rather than learning the experiment's archived state via a 409.
    experimentAuthorization.assertCanModify(existingExperiment, requestingUserId, requestingUserRole);

    // 3. Check Immutable State (Archived experiments are frozen)
    if (existingExperiment.getArchivedAt() != null) {
      log.warn("Attempted to update archived experiment {} by user {}", experimentId, requestingUserId);
      throw new ResourceConflictException("Cannot update an archived experiment. Please restore it first.");
    }

    // 4. Semantic Validation (Lookups for new DTO data) - validated and resolved via
    // ExperimentValidator, which returns the entities so we don't have to re-fetch them here.
    ExperimentValidator.ValidatedReferences validated = experimentValidator.validateForUpdate(experimentDto);
    User scientist = validated.scientist();
    Reactant reactantA = validated.reactantA();
    Reactant reactantB = validated.reactantB();
    Solvent solvent = validated.solvent();
    Instrument instrument = validated.instrument();
    Modality modality = validated.modality();

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
}
