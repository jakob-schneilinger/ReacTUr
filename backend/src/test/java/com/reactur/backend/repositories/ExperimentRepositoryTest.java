package com.reactur.backend.repositories;

import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ExperimentRepository}'s custom queries against a real database: filtering to
 * only archived experiments, and finding a user's archived experiments by creator or scientist.
 */
// See ReactantRepositoryTest for why @DataJpaTest + @AutoConfigureTestDatabase(Replace.NONE) +
// @ActiveProfiles("test") are all required together.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ExperimentRepositoryTest {

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ReactantRepository reactantRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String prefix) {
        return userRepository.saveAndFlush(User.builder()
                .id("test-user-" + prefix + "-" + UUID.randomUUID())
                .email(prefix + "@example.com")
                .build());
    }

    private Reactant persistReactant(String shortName, User createdBy) {
        Reactant reactant = Reactant.builder()
                .shortName(shortName)
                .smiles("C")
                .createdBy(createdBy)
                .build();
        return reactantRepository.saveAndFlush(reactant);
    }

    private Experiment persistExperiment(User createdBy, User scientist, Instant archivedAt) {
        Reactant reactantA = persistReactant("Reactant A " + UUID.randomUUID(), createdBy);
        Reactant reactantB = persistReactant("Reactant B " + UUID.randomUUID(), createdBy);

        Metadata metadata = Metadata.builder()
                .date(LocalDate.now())
                .scientist(scientist)
                .build();

        ExperimentResult result = ExperimentResult.builder()
                .rateConstant(1.0)
                .errorValue(0.1)
                .isErrorPercentage(false)
                .build();

        Experiment experiment = Experiment.builder()
                .reactantA(reactantA)
                .reactantB(reactantB)
                .metadata(metadata)
                .result(result)
                .createdBy(createdBy)
                .archivedAt(archivedAt)
                .build();
        return experimentRepository.saveAndFlush(experiment);
    }

    @Test
    void findByArchivedAtIsNotNull_ShouldReturnOnlyArchivedExperiments() {
        User userA = persistUser("user-a");
        Experiment archived = persistExperiment(userA, userA, Instant.now());
        persistExperiment(userA, userA, null);

        List<Experiment> result = experimentRepository.findByArchivedAtIsNotNull();

        assertEquals(1, result.size());
        assertEquals(archived.getId(), result.get(0).getId());
    }

    @Test
    void findByArchivedAtIsNotNull_ShouldReturnEmptyList_WhenNoExperimentsAreArchived() {
        User userA = persistUser("user-a");
        persistExperiment(userA, userA, null);

        List<Experiment> result = experimentRepository.findByArchivedAtIsNotNull();

        assertTrue(result.isEmpty());
    }

    @Test
    void findArchivedExperimentsForUser_ShouldReturnExperiment_WhenUserIsCreator() {
        User userA = persistUser("user-a");
        User userB = persistUser("user-b");
        Experiment experiment = persistExperiment(userA, userB, Instant.now());

        List<Experiment> result = experimentRepository.findArchivedExperimentsForUser(userA.getId());

        assertEquals(1, result.size());
        assertEquals(experiment.getId(), result.get(0).getId());
    }

    @Test
    void findArchivedExperimentsForUser_ShouldReturnExperiment_WhenUserIsScientist() {
        User userA = persistUser("user-a");
        User userB = persistUser("user-b");
        Experiment experiment = persistExperiment(userB, userA, Instant.now());

        List<Experiment> result = experimentRepository.findArchivedExperimentsForUser(userA.getId());

        assertEquals(1, result.size());
        assertEquals(experiment.getId(), result.get(0).getId());
    }

    @Test
    void findArchivedExperimentsForUser_ShouldExcludeUnarchivedExperiments_ForSameUser() {
        User userA = persistUser("user-a");
        persistExperiment(userA, userA, null);

        List<Experiment> result = experimentRepository.findArchivedExperimentsForUser(userA.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findArchivedExperimentsForUser_ShouldReturnEmptyList_WhenUserIsNeitherCreatorNorScientist() {
        User userA = persistUser("user-a");
        User userC = persistUser("user-c");
        persistExperiment(userC, userC, Instant.now());

        List<Experiment> result = experimentRepository.findArchivedExperimentsForUser(userA.getId());

        assertTrue(result.isEmpty());
    }
}
