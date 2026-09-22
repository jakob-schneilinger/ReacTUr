package com.reactur.backend.repositories.options;

import com.reactur.backend.entities.options.ChemicalType;
import org.springframework.stereotype.Repository;

/** Concrete repository bean for {@link ChemicalType} options; all query logic lives in {@link OptionRepository}. */
@Repository
public interface ChemicalTypeRepository extends OptionRepository<ChemicalType> {
}
