package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.reactants.ReactantDto;
import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.UpdateReactantDto;
import com.reactur.backend.services.ReactantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/reactant")
@RequiredArgsConstructor
public class ReactantEndpoint {

    private final ReactantService reactantService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ReactantDetailDto> createReactant(@Valid @RequestBody CreateReactantDto saveReactant, @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /api/reactant called by user {}. ShortName: {}", jwt.getSubject(), saveReactant.shortName());
        ReactantDetailDto created = reactantService.createReactant(saveReactant, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReactantDetailDto> updateReactant(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateReactantDto saveReactant,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PATCH /api/reactant/{} called by user {}", uuid, jwt.getSubject());
        ReactantDetailDto updated = reactantService.updateReactant(uuid, saveReactant);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ReactantDetailDto> getReactantDetail(@PathVariable UUID uuid, @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/reactant/{} called by user {}", uuid, jwt.getSubject());
        ReactantDetailDto detail = reactantService.getReactantDetail(uuid);
        return ResponseEntity.ok(detail);
    }

    // TODO-Future: For future versions, consider adding a BackEnd Reactant substructure search
    //  as Reactants Amount grows in time, to avoid FrontEnd / Browser overload

    @GetMapping
    public ResponseEntity<List<ReactantDto>> getAllReactants(@AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/reactant called by user {}", jwt.getSubject());
        List<ReactantDto> reactants = reactantService.getAllReactants();
        return ResponseEntity.ok(reactants);
    }

    @GetMapping("/archived")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<ReactantDto>> getArchivedReactants(@AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/reactant/archived called by user {}", jwt.getSubject());
        List<ReactantDto> archived = reactantService.getArchivedReactants();
        return ResponseEntity.ok(archived);
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archiveReactant(@PathVariable UUID uuid, @AuthenticationPrincipal Jwt jwt) {
        log.info("DELETE /api/reactant/{} called by user {}", uuid, jwt.getSubject());
        reactantService.archiveReactant(uuid);
        return ResponseEntity.noContent().build();
    }

    // Restoring a reactant is treated as archive-management, which stays ADMIN-only -
    // SUPERVISOR may create reactants but not undo an ADMIN's archive decision.
    @PutMapping("/{uuid}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReactantDto> restoreReactant(@PathVariable UUID uuid, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /api/reactant/{}/restore called by user {}", uuid, jwt.getSubject());
        ReactantDto restored = reactantService.restoreReactant(uuid);
        return ResponseEntity.ok(restored);
    }
}
