package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.ExperimentDtos.GetExperimentDto;
import com.reactur.backend.dtos.ExperimentDtos.SaveExperimentDto;
import com.reactur.backend.entities.AuditLog;
import com.reactur.backend.entities.ExperimentEntities.Experiment;
import com.reactur.backend.entities.ExperimentEntities.ExperimentResult;
import com.reactur.backend.entities.ExperimentEntities.Metadata;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.SelectionOptions.Instrument;
import com.reactur.backend.entities.SelectionOptions.Modality;
import com.reactur.backend.entities.SelectionOptions.Solvent;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.AuditAction;
import com.reactur.backend.exeptions.ResourceNotFoundException;
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
  public GetExperimentDto createExperiment(SaveExperimentDto experimentDto, String userId) {
    log.trace("Attempting to create new experiment with DOI: {} for user ID: {}", experimentDto.doi(), userId);

    // First, semantic Validation

    // Who physically clicks 'Save': (Extracted from Keycloak Token)
    User creator = userRepository.findById(userId)
      .orElseThrow(() -> {
        log.error(
          "CRITICAL: Authenticated Keycloak user {} not found in internal database. " +
            "UserSyncFilter may have failed.", userId
        );
        return new IllegalStateException("System state error: Authenticated user lacks internal profile.");
      });

    // Who actually performed the experiment: (Extracted from DTO payload)
    User scientist = userRepository.findById(experimentDto.metadata().scientist())
      .orElseThrow(() -> new ResourceNotFoundException(
        "Scientist with ID " + experimentDto.metadata().scientist() + " not found."
      ));

    // TODO: Move to Validator? We can discuss Later

    Reactant reactantA = reactantRepository.findById(experimentDto.reactantA())
      .orElseThrow(() -> new ResourceNotFoundException("Reactant A does not exist."));

    Reactant reactantB = reactantRepository.findById(experimentDto.reactantB())
      .orElseThrow(() -> new ResourceNotFoundException("Reactant B does not exist."));

    Solvent solvent = solventRepository.findById(experimentDto.metadata().solvent())
      .orElseThrow(() -> new ResourceNotFoundException("Solvent does not exist."));

    Instrument instrument = null;
    if (experimentDto.metadata().instrument() != null) {
      instrument = instrumentRepository.findById(experimentDto.metadata().instrument())
        .orElseThrow(() -> new ResourceNotFoundException("Instrument does not exist."));
    }

    Modality modality = null;
    if (experimentDto.metadata().modality() != null) {
      modality = modalityRepository.findById(experimentDto.metadata().modality())
        .orElseThrow(() -> new ResourceNotFoundException("Modality does not exist."));
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
      .userId(userId)
      .action(AuditAction.CREATE)
      // TODO: Decide how to serialize the JSON diffs for changeFrom/changeTo
      .build();
    auditLogRepository.save(auditLog);

    // ...finally returning the saved element

    return experimentMapper.experimentToGetExperimentDto(savedExperiment);
  }

}
