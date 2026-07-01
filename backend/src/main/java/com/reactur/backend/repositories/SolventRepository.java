package com.reactur.backend.repositories;
import com.reactur.backend.entities.SelectionOptions.Solvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface SolventRepository extends JpaRepository<Solvent, UUID> {}
