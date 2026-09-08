package com.reactur.backend.client;

import com.reactur.backend.enums.JobStatus;
import com.reactur.backend.exceptions.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StructureApiClientImpl implements StructureApiClient {

    private final RestClient restClient;

    public StructureApiClientImpl(@Value("${external-api.reactur.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public String triggerJob(String smiles) {
        try {
            TriggerResponse response = restClient.post()
                    .uri("/jobs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("smiles", smiles))
                    .retrieve()
                    .body(TriggerResponse.class);

            if (response == null || response.job_id() == null) {
                throw new ExternalApiException("External API returned empty or invalid response");
            }

            return response.job_id();

        } catch (RestClientResponseException e) {
            throw new ExternalApiException(
                    "Structure API is currently unavailable or returned an error. Failed to trigger job for smiles '%s'. HTTP Status: %s. Body: %s"
                            .formatted(smiles, e.getStatusCode(), e.getResponseBodyAsString()),
                    e);
        } catch (Exception e) {
            throw new ExternalApiException(
                    "Could not reach the Structure API. Network or parsing error while triggering job for smiles '%s'."
                            .formatted(smiles),
                    e);
        }
    }

    @Override
    public JobStatusResult checkJobStatus(String jobId) {
        try {
            StatusResponse response = restClient.get()
                    .uri("/jobs/{jobId}", jobId)
                    .retrieve()
                    .body(StatusResponse.class);

            if (response == null) {
                return new JobStatusResult(JobStatus.FAILED, null, "Empty response from API");
            }

            List<String> structures = null;
            if (response.result() != null && response.result().structures() != null) {
                structures = response.result().structures();
            }

            JobStatus parsedStatus = JobStatus.fromString(response.status());

            return new JobStatusResult(parsedStatus, structures, response.error());

        } catch (RestClientResponseException e) {
            throw new ExternalApiException(
                    "Error while checking job status from external API. Job ID: '%s'. HTTP Status: %s."
                            .formatted(jobId, e.getStatusCode()),
                    e);
        } catch (Exception e) {
            throw new ExternalApiException(
                    "Could not reach the Structure API. Network error while checking job status for job ID '%s'."
                            .formatted(jobId),
                    e);
        }
    }

    private record TriggerResponse(String job_id, String status, String status_url) {}

    private record StatusResponse(String id, String status, ResultData result, String error) {}

    private record ResultData(int structure_count, List<String> structures) {}
}
