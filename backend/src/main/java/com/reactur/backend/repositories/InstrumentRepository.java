package com.reactur.backend.repositories;
import com.reactur.backend.entities.SelectionOptions.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {}
