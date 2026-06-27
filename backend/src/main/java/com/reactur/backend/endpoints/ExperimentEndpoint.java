package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.AuditLogDto;
import com.reactur.backend.dtos.ExperimentDtos.GetExperimentDto;
import com.reactur.backend.dtos.ExperimentDtos.SaveExperimentDto;
import com.reactur.backend.dtos.ExperimentDtos.SearchParametersDto;
import com.reactur.backend.services.ExperimentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/experiment")
public class ExperimentEndpoint {

    private final ExperimentService experimentService;

    // TODO: Decide how to do with archived experiments
    //  - Should archived experiments be visible to everybody, or just to admins (+ the original creator)?
    //  - For Example, a different api for archived experiments, like get .../archived/uuid instead of directly /uuid ?
    //  - Advantage: Easy to set who is allowed to access the /archived endpoint
    //  - Disadvantage: Necessity for two endpoints with similar (but not equal) functionality

    // Not sure if we should describe in the contract here or in the service
    // layer when something is correctly validated, etc.
    // We are testing syntax in the REST layer with the simple DTO annotations, and Semantic or Data-Base related
    // rules with the Service layer
    // Also: I'm not sure how we should verify permissions / security
    /**
     *
     * @param experimentDto
     * @return
     */
    @PostMapping
    public ResponseEntity<GetExperimentDto> createExperiment(
      @Valid @RequestBody SaveExperimentDto experimentDto,
      @AuthenticationPrincipal Jwt jwt  // Authentication data should come from the token, not from the user's request
    ) {

        // TODO: Remove get Jwt here and use getCurrentUser form UserService in Service

        String userId = jwt.getSubject();

        log.info("POST /api/experiment called by User ID: {}. ReactantA: {}, ReactantB: {}",
          userId, experimentDto.reactantA(), experimentDto.reactantB());

        GetExperimentDto createdExperiment = experimentService.createExperiment(experimentDto, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdExperiment);
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
