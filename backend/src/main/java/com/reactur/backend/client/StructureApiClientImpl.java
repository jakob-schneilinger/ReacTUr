package com.reactur.backend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class StructureApiClientImpl implements StructureApiClient{
    @Override
    public String triggerJob(String smiles) {
        return "";
    }

    @Override
    public List<String> checkJobStatus(String jobId) {
        return null;
    }
}
