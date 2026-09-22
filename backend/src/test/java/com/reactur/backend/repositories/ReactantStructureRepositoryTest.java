package com.reactur.backend.repositories;

import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.StructureFlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ReactantStructureRepository}'s custom queries against a real database: finding
 * structures by reactant (single and batch), and filtering by {@link StructureFlag}.
 */
// See ReactantRepositoryTest for why @DataJpaTest + @AutoConfigureTestDatabase(Replace.NONE) +
// @ActiveProfiles("test") are all required together.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ReactantStructureRepositoryTest {

    @Autowired
    private ReactantStructureRepository reactantStructureRepository;

    @Autowired
    private ReactantRepository reactantRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .id("test-user-" + UUID.randomUUID())
                .email("test-user@example.com")
                .build());
    }

    private Reactant persistReactant(String shortName) {
        Reactant reactant = Reactant.builder()
                .shortName(shortName)
                .smiles("C")
                .createdBy(testUser)
                .build();
        return reactantRepository.saveAndFlush(reactant);
    }

    private ReactantStructure persistStructure(Reactant reactant, StructureFlag flag) {
        ReactantStructure structure = ReactantStructure.builder()
                .reactant(reactant)
                .fileName("structure.mol")
                .structure("dummy-structure-payload")
                .flag(flag)
                .build();
        return reactantStructureRepository.saveAndFlush(structure);
    }

    @Test
    void findByReactant_Id_ShouldReturnStructuresBelongingToThatReactant() {
        Reactant reactant = persistReactant("Ethanol");
        ReactantStructure structure = persistStructure(reactant, StructureFlag.ACTIVE);

        Reactant otherReactant = persistReactant("Methanol");
        persistStructure(otherReactant, StructureFlag.ACTIVE);

        List<ReactantStructure> result = reactantStructureRepository.findByReactant_Id(reactant.getId());

        assertEquals(1, result.size());
        assertEquals(structure.getId(), result.get(0).getId());
    }

    @Test
    void findByReactant_Id_ShouldReturnEmptyList_WhenReactantHasNoStructures() {
        Reactant reactant = persistReactant("Ethanol");

        List<ReactantStructure> result = reactantStructureRepository.findByReactant_Id(reactant.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByFlag_ShouldReturnStructuresWithMatchingFlag() {
        Reactant reactant = persistReactant("Ethanol");
        ReactantStructure active = persistStructure(reactant, StructureFlag.ACTIVE);
        persistStructure(reactant, StructureFlag.PENDING);

        List<ReactantStructure> result = reactantStructureRepository.findByFlag(StructureFlag.ACTIVE);

        assertEquals(1, result.size());
        assertEquals(active.getId(), result.get(0).getId());
    }

    @Test
    void findByFlag_ShouldReturnEmptyList_WhenNoStructuresHaveThatFlag() {
        Reactant reactant = persistReactant("Ethanol");
        persistStructure(reactant, StructureFlag.PENDING);

        List<ReactantStructure> result = reactantStructureRepository.findByFlag(StructureFlag.REJECTED);

        assertTrue(result.isEmpty());
    }
}
