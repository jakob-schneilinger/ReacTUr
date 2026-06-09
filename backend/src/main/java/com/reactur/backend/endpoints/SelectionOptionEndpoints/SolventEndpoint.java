package com.reactur.backend.endpoints.SelectionOptionEndpoints;

import com.reactur.backend.dtos.SelectionOption.SolventDto;
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
@RequestMapping("/api/solvent")
@RequiredArgsConstructor
public class SolventEndpoint {

    @GetMapping()
    public ResponseEntity<SolventDto[]> getKnownSolvents() {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PostMapping()
    public ResponseEntity<SolventDto[]> createSolvents(SolventDto instrument) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> archiveSolvent(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PutMapping("/{uuid}/restore")
    public ResponseEntity<Void> restoreSolvent(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }
}
