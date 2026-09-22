package com.reactur.backend.repositories;

import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentAttachment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import org.junit.jupiter.api.BeforeEach;
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
 * Verifies {@link ExperimentAttachmentRepository}'s custom query against a real database: only
 * an experiment's non-archived attachments are returned.
 */
// See ReactantRepositoryTest for why @DataJpaTest + @AutoConfigureTestDatabase(Replace.NONE) +
// @ActiveProfiles("test") are all required together.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ExperimentAttachmentRepositoryTest {

    @Autowired
    private ExperimentAttachmentRepository experimentAttachmentRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

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

    private Experiment persistExperiment() {
        Reactant reactantA = persistReactant("Reactant A " + UUID.randomUUID());
        Reactant reactantB = persistReactant("Reactant B " + UUID.randomUUID());

        Metadata metadata = Metadata.builder()
                .date(LocalDate.now())
                .scientist(testUser)
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
                .createdBy(testUser)
                .build();
        return experimentRepository.saveAndFlush(experiment);
    }

    private ExperimentAttachment persistAttachment(Experiment experiment, Instant archivedAt) {
        ExperimentAttachment attachment = ExperimentAttachment.builder()
                .experiment(experiment)
                .fileName("results.csv")
                .fileType("text/csv")
                .storageReferenceId(UUID.randomUUID().toString())
                .archivedAt(archivedAt)
                .build();
        return experimentAttachmentRepository.saveAndFlush(attachment);
    }

    @Test
    void findByExperimentIdAndArchivedAtIsNull_ShouldReturnAttachment_WhenNotArchived() {
        Experiment experiment = persistExperiment();
        ExperimentAttachment attachment = persistAttachment(experiment, null);

        List<ExperimentAttachment> result =
                experimentAttachmentRepository.findByExperimentIdAndArchivedAtIsNull(experiment.getId());

        assertEquals(1, result.size());
        assertEquals(attachment.getId(), result.get(0).getId());
    }

    @Test
    void findByExperimentIdAndArchivedAtIsNull_ShouldExcludeArchivedAttachments() {
        Experiment experiment = persistExperiment();
        persistAttachment(experiment, Instant.now());

        List<ExperimentAttachment> result =
                experimentAttachmentRepository.findByExperimentIdAndArchivedAtIsNull(experiment.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByExperimentIdAndArchivedAtIsNull_ShouldReturnEmptyList_WhenExperimentHasNoAttachments() {
        Experiment experiment = persistExperiment();

        List<ExperimentAttachment> result =
                experimentAttachmentRepository.findByExperimentIdAndArchivedAtIsNull(experiment.getId());

        assertTrue(result.isEmpty());
    }
}
