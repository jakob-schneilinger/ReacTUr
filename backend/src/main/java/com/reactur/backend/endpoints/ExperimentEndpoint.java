package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.AuditLogDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.enums.Role;
import com.reactur.backend.services.ExperimentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    // TODO: JavaDoc

    private final ExperimentService experimentService;

    @PostMapping
    public ResponseEntity<GetExperimentDto> createExperiment(
      @Valid @RequestBody SaveExperimentDto experimentDto,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("POST /api/experiment called by User ID: {}. ReactantA: {}, ReactantB: {}",
          jwt.getSubject(), experimentDto.reactantA(), experimentDto.reactantB());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto createdExperiment = experimentService.createExperiment(experimentDto, jwt.getSubject(), userRole);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdExperiment);
    }

    // Javadoc: Just the creator or the scientist, or an admin, are allowed to update the experiment
    // Notes:
    @PutMapping("/{uuid}")
    public ResponseEntity<GetExperimentDto> updateExperiment(
      @PathVariable UUID uuid,
      @Valid @RequestBody SaveExperimentDto experimentDto,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("PUT /api/experiment/{} called by User ID: {}", uuid, jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto updatedExperiment = experimentService.updateExperiment(uuid, experimentDto, jwt.getSubject(), userRole);

        return ResponseEntity.ok(updatedExperiment);
    }

    // If archived, just the creator, scientist or an admin can retrieve the experiment.
    @GetMapping("/{uuid}")
    public ResponseEntity<GetExperimentDto> getExperiment(
      @PathVariable UUID uuid,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("GET /api/experiment/{} called by User ID: {}", uuid, jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto experiment = experimentService.getExperiment(uuid, jwt.getSubject(), userRole);

        return ResponseEntity.ok(experiment);
    }

    // Should archived-experiments also show up? ---> Let's say no
    @GetMapping()
    public ResponseEntity<GetExperimentDto[]> searchExperiments(SearchParametersDto searchParams) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    // TODO: Endpoint to get Logs by *Date*, by *User* and by Experiment
    //  Current one: Logs related to experiment with uuid
    @GetMapping("/{uuid}/audit_logs")
    public ResponseEntity<AuditLogDto[]> getAuditLogs(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    // Javadoc: I decided that normal users get the archived Experiments where they are the creator or the scientist...
    //  and ADMINS get all archived experiments that exist
    @GetMapping("/archived")
    public ResponseEntity<GetExperimentDto[]> getArchivedExperiments(
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("GET /api/experiment/archived called by User ID: {}", jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto[] archivedExperiments = experimentService.getArchivedExperiments(jwt.getSubject(), userRole);

        return ResponseEntity.ok(archivedExperiments);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> archiveExperiment(
      @PathVariable UUID uuid,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("DELETE (Archive) /api/experiment/{} called by User ID: {}", uuid, jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        experimentService.archiveExperiment(uuid, jwt.getSubject(), userRole);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{uuid}/restore")
    public ResponseEntity<GetExperimentDto> restoreExperiment(
      @PathVariable UUID uuid,
      Authentication authentication,
      @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("PUT (Restore) /api/experiment/{} called by User ID: {}", uuid, jwt.getSubject());

        Role userRole = extractUserRole(authentication);
        GetExperimentDto restoredExperiment = experimentService.restoreExperiment(uuid, jwt.getSubject(), userRole);

        return ResponseEntity.ok(restoredExperiment);
    }

    // TODO: Rewrite
    private Role extractUserRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return Role.USER;
        }
        boolean isAdmin = authentication.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return isAdmin ? Role.ADMIN : Role.USER;
    }
}
