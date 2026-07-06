package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.ExperimentHistoryDto;

import java.util.List;
import java.util.UUID;

public interface ExperimentHistoryService {

    List<ExperimentHistoryDto> getExperimentHistory(UUID experimentUuid);
}
