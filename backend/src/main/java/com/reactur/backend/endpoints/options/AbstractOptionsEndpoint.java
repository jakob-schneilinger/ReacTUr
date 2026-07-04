package com.reactur.backend.endpoints.options;

import com.reactur.backend.dtos.options.OptionDto;
import com.reactur.backend.services.options.OptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

public abstract class AbstractOptionsEndpoint<T extends OptionDto> {

    private final OptionService<T> service;

    protected AbstractOptionsEndpoint(OptionService<T> service) {
        this.service = service;
    }

    @GetMapping()
    public ResponseEntity<List<T>> getKnownOptions() {
        return ResponseEntity.ok(service.getKnownOptions());
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<T>> createOptions(@RequestBody @Valid T optionDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOptions(optionDto));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archiveOption(@PathVariable UUID uuid) {
        service.archiveOption(uuid);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{uuid}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restoreOption(@PathVariable UUID uuid) {
        service.restoreOption(uuid);
        return ResponseEntity.ok().build();
    }
}
