package com.reactur.backend.repositories.options;
import com.reactur.backend.entities.options.Instrument;
import org.springframework.stereotype.Repository;

@Repository
public interface InstrumentRepository extends OptionRepository<Instrument> {}
