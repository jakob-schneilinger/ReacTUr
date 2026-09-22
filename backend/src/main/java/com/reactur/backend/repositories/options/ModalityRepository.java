package com.reactur.backend.repositories.options;
import com.reactur.backend.entities.options.Modality;
import org.springframework.stereotype.Repository;

/** Concrete repository bean for {@link Modality} options; all query logic lives in {@link OptionRepository}. */
@Repository
public interface ModalityRepository extends OptionRepository<Modality> {}
