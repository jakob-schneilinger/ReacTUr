package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.CreateReactantDto;
import com.reactur.backend.dtos.ReactantDtos.UpdateReactantDto;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReactantDetailDto> createReactant(@Valid @RequestBody CreateReactantDto saveReactant, @AuthenticationPrincipal Jwt jwt) {
        log.info("User {} is attempting to create a new Reactant: {}", jwt.getSubject(), saveReactant.shortName());
        ReactantDetailDto created = reactantService.createReactant(saveReactant, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReactantDetailDto> updateReactant(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateReactantDto saveReactant,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("User {} is attempting to update Reactant with UUID: {}", jwt.getSubject(), uuid);
        ReactantDetailDto updated = reactantService.updateReactant(uuid, saveReactant);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ReactantDetailDto> getReactantDetail(@PathVariable UUID uuid) {
        ReactantDetailDto detail = reactantService.getReactantDetail(uuid);
        return ResponseEntity.ok(detail);
    }

    @GetMapping
    public ResponseEntity<List<ReactantDto>> getAllReactants() {
        List<ReactantDto> reactants = reactantService.getAllReactants();
        return ResponseEntity.ok(reactants);
    }

    @GetMapping("/archived")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReactantDto>> getArchivedReactants() {
        List<ReactantDto> archived = reactantService.getArchivedReactants();
        return ResponseEntity.ok(archived);
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archiveReactant(@PathVariable UUID uuid, @AuthenticationPrincipal Jwt jwt) {
        log.info("User {} is attempting to archive Reactant with UUID: {}", jwt.getSubject(), uuid);
        reactantService.archiveReactant(uuid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{uuid}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReactantDto> restoreReactant(@PathVariable UUID uuid, @AuthenticationPrincipal Jwt jwt) {
        log.info("User {} is attempting to restore Reactant with UUID: {}", jwt.getSubject(), uuid);
        ReactantDto restored = reactantService.restoreReactant(uuid);
        return ResponseEntity.ok(restored);
    }
}
