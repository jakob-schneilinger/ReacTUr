package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

}
