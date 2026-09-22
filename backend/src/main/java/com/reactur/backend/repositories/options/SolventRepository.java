package com.reactur.backend.repositories.options;
import com.reactur.backend.entities.options.Solvent;
import org.springframework.stereotype.Repository;

/** Concrete repository bean for {@link Solvent} options; all query logic lives in {@link OptionRepository}. */
@Repository
public interface SolventRepository extends OptionRepository<Solvent> {}
