package com.reactur.backend.endpoints;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:5173") //TODO: security config
public class TestEndpoint {

    private final RestClient restClient = RestClient.create();

    @GetMapping
    public ResponseEntity<String> test() {
        return new ResponseEntity<>("Hello World", HttpStatus.OK);
    }

    @GetMapping("/rdkit")
    public ResponseEntity<String> pingAllServices() {
        try {
            String pythonResponse = restClient.get()
                    .uri("http://localhost:8000/api/python-ping")
                    .retrieve()
                    .body(String.class);

            String combinedResult = "🟢 Java Backend ist erreichbar! | Weitergeleitete Antwort: " + pythonResponse;

            return ResponseEntity.ok(combinedResult);

        } catch (Exception e) {
            String errorResult = "🟢 Java Backend ist erreichbar! | 🔴 Aber Python RDKit Service antwortet nicht: " + e.getMessage();
            return ResponseEntity.status(500).body(errorResult);
        }
    }
}


