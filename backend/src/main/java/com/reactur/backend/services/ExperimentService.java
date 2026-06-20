package com.reactur.backend.services;

import com.reactur.backend.dtos.ExperimentDtos.GetExperimentDto;
import com.reactur.backend.dtos.ExperimentDtos.SaveExperimentDto;
import org.springframework.stereotype.Service;

@Service
public interface ExperimentService {

  // TODO: JavaDOC
  GetExperimentDto createExperiment(SaveExperimentDto experimentDto, String userId);

}

