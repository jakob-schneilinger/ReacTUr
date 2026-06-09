package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.SaveReactantDto;
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
@RequestMapping("/api/reactant")
@RequiredArgsConstructor
public class ReactantEndpoint {

    @PostMapping
    public ResponseEntity<ReactantDetailDto> createReactant(SaveReactantDto saveReactant) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PatchMapping("/{uuid}")
    public ResponseEntity<ReactantDetailDto> updateReactant(@PathVariable UUID uuid, SaveReactantDto saveReactant) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ReactantDetailDto> getReactantDetail(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @GetMapping
    public ResponseEntity<ReactantDto[]> getAllReactants() {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @GetMapping("/archived")
    public ResponseEntity<ReactantDto[]> getArchivedReactants() {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> archiveReactant(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PutMapping("/{uuid}/restore")
    public ResponseEntity<ReactantDto> restoreReactant(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }
}
