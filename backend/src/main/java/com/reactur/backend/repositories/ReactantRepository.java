package com.reactur.backend.repositories;

import com.reactur.backend.entities.Reactant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReactantRepository extends JpaRepository<Reactant, UUID> {
}
