package com.reactur.backend.repositories;

import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentFolder;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.enums.Role;
import com.reactur.backend.enums.StructureFlag;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves that the {@code @Version} fields added to Reactant, Experiment, ReactantStructure, User and
 * ExperimentFolder (see the "Race condition inventory" item in Software_todo.md) actually cause Hibernate to
 * reject a stale concurrent write, instead of silently overwriting it.
 * <p>
 * This can only be proven against a real database (Mockito mocks don't simulate Hibernate's version-check
 * behavior at all - a mocked {@code save()} just returns whatever it's told to). The thread-free way to
 * reproduce a lost-update race deterministically: load the SAME row into two separate, fully-DETACHED
 * snapshots, save one of them (bumping the DB's version), then try to save the other stale one, which should
 * fail. Getting this right requires {@link EntityManager#clear()} after EVERY read/write, not just between the
 * two reads - if the two snapshots are ever both attached to the persistence context at once, Hibernate's
 * first-level cache silently reuses/merges them into a single managed instance (since a persistence context can
 * only hold one managed object per entity identity), which defeats the whole test by keeping them in sync
 * instead of letting them diverge.
 * <p>
 * See {@code ReactantRepositoryTest}/{@code ExperimentRepositoryTest} for why
 * {@code @DataJpaTest} + {@code @AutoConfigureTestDatabase(Replace.NONE)} + {@code @ActiveProfiles("test")} are
 * all required together.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OptimisticLockingTest {

    @Autowired
    private ReactantRepository reactantRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ReactantStructureRepository structureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExperimentFolderRepository folderRepository;

    @Autowired
    private EntityManager entityManager;

    private User persistUser(String prefix) {
        User user = userRepository.saveAndFlush(User.builder()
                .id("test-user-" + prefix + "-" + UUID.randomUUID())
                .email(prefix + "@example.com")
                .build());
        entityManager.clear();
        return user;
    }

    @Test
    void reactant_concurrentUpdate_shouldFailOnStaleSave() {
        User creator = persistUser("creator");
        Reactant saved = reactantRepository.saveAndFlush(Reactant.builder()
                .shortName("Ethanol " + UUID.randomUUID())
                .smiles("CCO")
                .createdBy(creator)
                .build());
        entityManager.clear();

        Reactant firstCopy = reactantRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();
        Reactant secondCopy = reactantRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        firstCopy.setComment("Edited by request A");
        reactantRepository.saveAndFlush(firstCopy);
        entityManager.clear();

        secondCopy.setSmiles("CCCO");
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> reactantRepository.saveAndFlush(secondCopy));
    }

    @Test
    void experiment_concurrentUpdate_shouldFailOnStaleSave() {
        User creator = persistUser("creator");
        Reactant reactantA = reactantRepository.saveAndFlush(Reactant.builder()
                .shortName("Reactant A " + UUID.randomUUID()).smiles("C").createdBy(creator).build());
        Reactant reactantB = reactantRepository.saveAndFlush(Reactant.builder()
                .shortName("Reactant B " + UUID.randomUUID()).smiles("C").createdBy(creator).build());
        entityManager.clear();

        Experiment saved = experimentRepository.saveAndFlush(Experiment.builder()
                .reactantA(reactantA)
                .reactantB(reactantB)
                .metadata(Metadata.builder().date(LocalDate.now()).scientist(creator).build())
                .result(ExperimentResult.builder().rateConstant(1.0).errorValue(0.1).isErrorPercentage(false).build())
                .createdBy(creator)
                .build());
        entityManager.clear();

        // This is exactly the shape of the real bug: one "transaction" (updateExperiment) edits an unrelated
        // field, the other (archiveExperiment) sets archivedAt - without @Version, the first one's stale save
        // would have silently overwritten the second one's archivedAt back to null.
        Experiment updateAttempt = experimentRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();
        Experiment archiveAttempt = experimentRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        archiveAttempt.setArchivedAt(java.time.Instant.now());
        experimentRepository.saveAndFlush(archiveAttempt);
        entityManager.clear();

        updateAttempt.setDois(java.util.List.of("10.1000/example"));
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> experimentRepository.saveAndFlush(updateAttempt));
    }

    @Test
    void reactantStructure_concurrentUpdate_shouldFailOnStaleSave() {
        User creator = persistUser("creator");
        Reactant reactant = reactantRepository.saveAndFlush(Reactant.builder()
                .shortName("Ethanol " + UUID.randomUUID()).smiles("CCO").createdBy(creator).build());
        entityManager.clear();
        ReactantStructure saved = structureRepository.saveAndFlush(ReactantStructure.builder()
                .reactant(reactant)
                .flag(StructureFlag.PENDING)
                .externalJobId("job-1")
                .build());
        entityManager.clear();

        ReactantStructure firstCopy = structureRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();
        ReactantStructure secondCopy = structureRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        firstCopy.setFlag(StructureFlag.FAILED);
        structureRepository.saveAndFlush(firstCopy);
        entityManager.clear();

        secondCopy.setFlag(StructureFlag.READY);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> structureRepository.saveAndFlush(secondCopy));
    }

    @Test
    void user_concurrentRoleChange_shouldFailOnStaleSave() {
        User saved = persistUser("target");

        User firstCopy = userRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();
        User secondCopy = userRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        firstCopy.setRole(Role.ADMIN);
        userRepository.saveAndFlush(firstCopy);
        entityManager.clear();

        secondCopy.setRole(Role.SUPERVISOR);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> userRepository.saveAndFlush(secondCopy));
    }

    @Test
    void experimentFolder_concurrentUpdate_shouldFailOnStaleSave() {
        User owner = persistUser("owner");
        ExperimentFolder saved = folderRepository.saveAndFlush(ExperimentFolder.builder()
                .name("My Folder")
                .owner(owner)
                .build());
        entityManager.clear();

        ExperimentFolder firstCopy = folderRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();
        ExperimentFolder secondCopy = folderRepository.findById(saved.getId()).orElseThrow();
        entityManager.clear();

        firstCopy.setName("Renamed by request A");
        folderRepository.saveAndFlush(firstCopy);
        entityManager.clear();

        secondCopy.setName("Renamed by request B");
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> folderRepository.saveAndFlush(secondCopy));
    }

    @Test
    void reactant_version_incrementsOnEachSave() {
        User creator = persistUser("creator");
        Reactant saved = reactantRepository.saveAndFlush(Reactant.builder()
                .shortName("Ethanol " + UUID.randomUUID())
                .smiles("CCO")
                .createdBy(creator)
                .build());
        Long initialVersion = saved.getVersion();

        saved.setComment("First edit");
        Reactant afterFirstEdit = reactantRepository.saveAndFlush(saved);

        assertEquals(initialVersion + 1, afterFirstEdit.getVersion(),
                "Hibernate should increment @Version by exactly 1 on each successful update");
    }
}
