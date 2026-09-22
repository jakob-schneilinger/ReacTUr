package com.reactur.backend.repositories;

import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.ExperimentFolder;
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
 * Verifies {@link ExperimentFolderRepository}'s custom query against a real database: a user
 * only ever gets back folders they own.
 */
// See ReactantRepositoryTest for why @DataJpaTest + @AutoConfigureTestDatabase(Replace.NONE) +
// @ActiveProfiles("test") are all required together.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ExperimentFolderRepositoryTest {

    @Autowired
    private ExperimentFolderRepository experimentFolderRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = persistUser("owner");
        otherUser = persistUser("other-user");
    }

    private User persistUser(String prefix) {
        return userRepository.saveAndFlush(User.builder()
                .id("test-user-" + prefix + "-" + UUID.randomUUID())
                .email(prefix + "@example.com")
                .build());
    }

    private ExperimentFolder persistFolder(String name, User folderOwner) {
        ExperimentFolder folder = ExperimentFolder.builder()
                .name(name)
                .owner(folderOwner)
                .build();
        return experimentFolderRepository.saveAndFlush(folder);
    }

    @Test
    void findAllByOwner_Id_ShouldReturnFoldersOwnedByUser() {
        ExperimentFolder folder = persistFolder("My Folder", owner);
        persistFolder("Someone Else's Folder", otherUser);

        List<ExperimentFolder> result = experimentFolderRepository.findAllByOwner_Id(owner.getId());

        assertEquals(1, result.size());
        assertEquals(folder.getId(), result.get(0).getId());
    }

    @Test
    void findAllByOwner_Id_ShouldReturnEmptyList_WhenUserOwnsNoFolders() {
        persistFolder("Someone Else's Folder", otherUser);

        List<ExperimentFolder> result = experimentFolderRepository.findAllByOwner_Id(owner.getId());

        assertTrue(result.isEmpty());
    }
}
