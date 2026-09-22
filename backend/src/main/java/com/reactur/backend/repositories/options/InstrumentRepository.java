package com.reactur.backend.repositories.options;
import com.reactur.backend.entities.options.Instrument;
import org.springframework.stereotype.Repository;

/** Concrete repository bean for {@link Instrument} options; all query logic lives in {@link OptionRepository}. */
@Repository
public interface InstrumentRepository extends OptionRepository<Instrument> {}
