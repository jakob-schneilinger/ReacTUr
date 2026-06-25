package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.SaveReactantDto;
import com.reactur.backend.services.ReactantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/reactant")
@RequiredArgsConstructor
public class ReactantEndpoint {

    private final ReactantService reactantService;

    @PostMapping
    public ResponseEntity<ReactantDetailDto> createReactant(@Valid @RequestBody SaveReactantDto saveReactant) {
        ReactantDetailDto created = reactantService.createReactant(saveReactant);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{uuid}")
    public ResponseEntity<ReactantDetailDto> updateReactant(@PathVariable UUID uuid, @Valid @RequestBody SaveReactantDto saveReactant) {
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
    public ResponseEntity<List<ReactantDto>> getArchivedReactants() {
        List<ReactantDto> archived = reactantService.getArchivedReactants();
        return ResponseEntity.ok(archived);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> archiveReactant(@PathVariable UUID uuid) {
        reactantService.archiveReactant(uuid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{uuid}/restore")
    public ResponseEntity<ReactantDto> restoreReactant(@PathVariable UUID uuid) {
        ReactantDto restored = reactantService.restoreReactant(uuid);
        return ResponseEntity.ok(restored);
    }
}
