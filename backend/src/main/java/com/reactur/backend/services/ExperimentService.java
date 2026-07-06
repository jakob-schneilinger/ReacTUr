package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface ExperimentService {
  // TODO: JavaDOC

  GetExperimentDto createExperiment(SaveExperimentDto experimentDto, String requestingUserId, Role requestingUserRole);

  // TODO: Clarify who has access to the archived experiment. Right now I set that only
  //  the creator, scientist, and admins can retrieve archived Experiments
  GetExperimentDto getExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole);

  void archiveExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole);

  GetExperimentDto restoreExperiment(UUID experimentId, String requestingUserId, Role requestingUserRole);

  GetExperimentDto[] getArchivedExperiments(String requestingUserId, Role requestingUserRole);

  GetExperimentDto updateExperiment(
    UUID experimentId,
    SaveExperimentDto experimentDto,
    String requestingUserId,
    Role requestingUserRole
  );

  Page<GetExperimentDto> searchExperiments(
    SearchParametersDto searchParams,
    Pageable pageable,
    String requestingUserId,
    Role requestingUserRole
  );

  /**
   * Retrieves the audit history (changelog) for a specific experiment.
   * Leverages JaVers to extract a chronologically sorted list of commits and their exact property changes.
   *
   * @param experimentUuid The unique identifier of the experiment to inspect.
   * @return A list of {@link ExperimentHistoryDto} objects representing the commit history,
   * sorted from newest to oldest. Returns an empty list if the experiment has no history.
   */
  List<ExperimentHistoryDto> getExperimentHistory(UUID experimentUuid);
}
