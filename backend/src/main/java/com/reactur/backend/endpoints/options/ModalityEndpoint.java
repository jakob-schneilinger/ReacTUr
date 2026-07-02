package com.reactur.backend.endpoints.options;

import com.reactur.backend.dtos.SelectionOption.ModalityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/modality")
@RequiredArgsConstructor
public class ModalityEndpoint {

    @GetMapping()
    public ResponseEntity<ModalityDto[]> getKnownModalities() {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PostMapping()
    public ResponseEntity<ModalityDto[]> createModalities(ModalityDto modality) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> archiveModality(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PutMapping("/{uuid}/restore")
    public ResponseEntity<Void> restoreModality(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }
}
