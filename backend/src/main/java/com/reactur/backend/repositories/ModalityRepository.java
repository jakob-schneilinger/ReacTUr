package com.reactur.backend.repositories;
import com.reactur.backend.entities.options.Modality;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ModalityRepository extends JpaRepository<Modality, UUID> {}
