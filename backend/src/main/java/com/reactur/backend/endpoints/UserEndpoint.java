package com.reactur.backend.endpoints;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.SetRoleDto;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserEndpoint {

    @GetMapping()
    public ResponseEntity<GetUserDto[]> getAllUsers() {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PostMapping("/{uuid}")
    public ResponseEntity<Void> setRole(@PathVariable UUID uuid, SetRoleDto setRole) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> archiveUser(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }

    @PutMapping("/{uuid}/restore")
    public ResponseEntity<Void> restoreUser(@PathVariable UUID uuid) {

        //TODO:
        throw new UnsupportedOperationException();
    }
}
