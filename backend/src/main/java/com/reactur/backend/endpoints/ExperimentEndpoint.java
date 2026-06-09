package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.AuditLogDto;
import com.reactur.backend.dtos.ExperimentDtos.GetExperimentDto;
import com.reactur.backend.dtos.ExperimentDtos.SaveExperimentDto;
import com.reactur.backend.dtos.ExperimentDtos.SearchParametersDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/experiment")
@RequiredArgsConstructor
public class ExperimentEndpoint {

    @PostMapping
    public ResponseEntity<GetExperimentDto> createExperiment(SaveExperimentDto experimentDto) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PatchMapping("/{uuid}")
    public ResponseEntity<GetExperimentDto> patchExperiment(@PathVariable UUID uuid, SaveExperimentDto experimentDto) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<GetExperimentDto> getExperiment(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @GetMapping()
    public ResponseEntity<GetExperimentDto[]> searchExperiments(SearchParametersDto searchParams) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @GetMapping("/{uuid}/audit_logs")
    public ResponseEntity<AuditLogDto[]> getAuditLogs(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @GetMapping("/archived")
    public ResponseEntity<GetExperimentDto[]> getArchivedExperiments() {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> archiveExperiment(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PutMapping("/{uuid}/restore")
    public ResponseEntity<GetExperimentDto> restoreExperiment(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }
}
