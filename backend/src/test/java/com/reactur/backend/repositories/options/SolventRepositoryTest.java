package com.reactur.backend.repositories.options;

import com.reactur.backend.entities.options.Solvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct test for {@code SolventRepository}, one of the {@code OptionRepository}-based option
 * repositories (ChemicalTypeRepository, InstrumentRepository, ModalityRepository,
 * SolventRepository). All four extend the {@code @NoRepositoryBean} generic
 * {@code OptionRepository} identically, and each has its own direct test file exercising
 * {@code existsByNameIgnoreCase} against its own concrete repository bean - this file no longer
 * stands in as a single representative for the other three.
 * <p>
 * See {@code ReactantRepositoryTest} for why {@code @DataJpaTest} +
 * {@code @AutoConfigureTestDatabase(Replace.NONE)} + {@code @ActiveProfiles("test")} are all
 * required together.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SolventRepositoryTest {

    @Autowired
    private SolventRepository solventRepository;

    private void persistSolvent(String name) {
        solventRepository.saveAndFlush(Solvent.builder().name(name).build());
    }

    @Test
    void existsByNameIgnoreCase_ShouldReturnTrue_WhenNameMatchesCaseInsensitively() {
        persistSolvent("Ethanol");

        assertTrue(solventRepository.existsByNameIgnoreCase("ethanol"));
    }

    @Test
    void existsByNameIgnoreCase_ShouldReturnFalse_WhenNoOptionMatches() {
        persistSolvent("Ethanol");

        assertFalse(solventRepository.existsByNameIgnoreCase("methanol"));
    }
}
