package com.reactur.backend.repositories.options;

import com.reactur.backend.entities.options.BaseOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

@NoRepositoryBean
public interface OptionRepository <E extends BaseOption> extends JpaRepository<E, UUID> {
}
