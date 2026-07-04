package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.ExperimentDtos.GetExperimentDto;
import com.reactur.backend.dtos.ExperimentDtos.SaveExperimentDto;
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
import com.reactur.backend.exeptions.ResourceNotFoundException;
import com.reactur.backend.exeptions.UnprocessableEntityException;
import com.reactur.backend.mappers.ExperimentMapper;
import com.reactur.backend.repositories.AuditLogRepository;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.InstrumentRepository;
import com.reactur.backend.repositories.ModalityRepository;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.SolventRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.services.ExperimentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reactur.backend.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;

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
  private final AuditLogRepository auditLogRepository;
  private final ExperimentMapper experimentMapper;

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

}
