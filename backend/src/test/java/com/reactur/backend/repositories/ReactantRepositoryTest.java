package com.reactur.backend.repositories;

import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO: Remove explanations:
// * {@code ReactantServiceTest} mocks {@link ReactantRepository} directly, so it can only prove that
// * {@code hasNameConflict}/{@code hasNameConflictExcludingId} are *called* with lowercased
// * arguments - it structurally cannot prove that the underlying JPQL (with its {@code LOWER(...)}
// * wrapping) is actually case-insensitive. Only a real query execution against a real database can
// * prove that, which is what this test does.
// * <p>
// * Activates the "test" Spring profile so that {@code src/test/resources/application-test.yml}
// * (H2 in-memory, {@code ddl-auto: create-drop}) is used, and disables Spring Boot's automatic
// * test-database replacement ({@code @AutoConfigureTestDatabase} is otherwise implied by
// * {@code @DataJpaTest} with {@code Replace.ANY}) so that config is actually honored rather than
// * being silently swapped out for an auto-configured embedded database. No other test in this
// * codebase activates the "test" profile/config yet, so this is the first to wire it up.
// */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ReactantRepositoryTest {

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

    private Reactant persistReactant(String shortName, List<String> additionalShortNames) {
        Reactant reactant = Reactant.builder()
                .shortName(shortName)
                .smiles("C")
                .additionalShortNames(additionalShortNames)
                .createdBy(testUser)
                .build();
        return reactantRepository.saveAndFlush(reactant);
    }

    @Test
    void hasNameConflict_ShouldReturnTrue_WhenShortNameMatchesExactCase() {
        persistReactant("Ethanol", List.of());

        assertTrue(reactantRepository.hasNameConflict(Set.of("ethanol")));
    }

    @Test
    void hasNameConflict_ShouldReturnTrue_WhenShortNameMatchesCaseInsensitively() {
        persistReactant("HCl", List.of());

        assertTrue(reactantRepository.hasNameConflict(Set.of("hcl")));
    }

    @Test
    void hasNameConflict_ShouldReturnTrue_WhenAdditionalShortNameMatchesCaseInsensitively() {
        persistReactant("Hydrochloric Acid", List.of("HCl"));

        assertTrue(reactantRepository.hasNameConflict(Set.of("hcl")));
    }

    @Test
    void hasNameConflict_ShouldReturnFalse_WhenNoNameMatches() {
        persistReactant("Ethanol", List.of("EtOH"));

        assertFalse(reactantRepository.hasNameConflict(Set.of("methanol")));
    }

    @Test
    void hasNameConflictExcludingId_ShouldReturnFalse_WhenOnlyMatchIsTheExcludedReactant() {
        Reactant reactant = persistReactant("HCl", List.of());

        assertFalse(reactantRepository.hasNameConflictExcludingId(Set.of("hcl"), reactant.getId()));
    }

    @Test
    void hasNameConflictExcludingId_ShouldReturnTrue_WhenAnotherReactantMatchesCaseInsensitively() {
        persistReactant("HCl", List.of());
        Reactant otherReactant = persistReactant("Ethanol", List.of());

        assertTrue(reactantRepository.hasNameConflictExcludingId(Set.of("hcl"), otherReactant.getId()));
    }

    @Test
    void hasNameConflictExcludingId_ShouldReturnFalse_WhenNoNameMatches() {
        Reactant reactant = persistReactant("Ethanol", List.of());

        assertFalse(reactantRepository.hasNameConflictExcludingId(Set.of("methanol"), reactant.getId()));
    }
}
