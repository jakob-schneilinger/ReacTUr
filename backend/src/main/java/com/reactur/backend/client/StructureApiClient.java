package com.reactur.backend.client;

import java.util.List;

public interface StructureApiClient {

    String triggerJob(String smiles);

    List<String> checkJobStatus(String jobId);
}
