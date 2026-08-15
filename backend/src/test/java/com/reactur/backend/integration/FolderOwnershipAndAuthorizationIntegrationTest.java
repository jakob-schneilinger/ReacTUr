package com.reactur.backend.integration;

import com.reactur.backend.client.StructureApiClient;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.dtos.folders.GetFolderDto;
import com.reactur.backend.dtos.folders.SaveFolderDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import software.amazon.awssdk.services.s3.S3Client;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier-1 integration tests covering INT-12 through INT-16 of
 * {@code backend/docs/INTEGRATION_TEST_SCENARIOS.md} - Folder Ownership (E) and the remainder of
 * the Authorization Matrix (F).
 * <p>
 * Follows exactly the pattern established by {@link ExperimentLifecycleIntegrationTest}: real
 * Spring context, real H2 database, real security filter chain, real
 * {@code CustomJwtAuthenticationConverter}, real services/repositories. See that class's Javadoc
 * for the full rationale on why {@code SecurityMockMvcRequestPostProcessors.jwt()} is never used
 * here, in favor of {@link #asRealJwtSubject(String)}.
 * <p>
 * Only three beans are mocked, for the same reason as the pilot: {@link JwtDecoder} (no real
 * Keycloak in this tier), {@link StructureApiClient} (external chemistry structure-generation
 * API, unused by these particular scenarios but still mocked for context-consistency with the
 * other classes in this tier), and {@link S3Client} (the SeaweedFS/S3 file storage backend).
 * <p>
 * <b>INT-15 (the "UNAUTHORIZED global gate" scenario) is intentionally NOT reimplemented here</b>
 * - it is already fully covered by
 * {@link ExperimentLifecycleIntegrationTest#scenarioC_realConverterDbRoleLookup_determinesAuthorizationOutcome()},
 * which proves a real {@code Role.UNAUTHORIZED} DB row is rejected by {@code SecurityConfig}'s
 * global gate. Nothing about that gate is Folder-specific, so duplicating it here would add no
 * genuine coverage.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        // Same rationale as ExperimentLifecycleIntegrationTest: SeaweedFsConfig/StructureApiClientImpl
        // require these @Value properties to be resolvable even though the beans hosting them are
        // @MockitoBean-overridden below, and application-test.yml intentionally doesn't define them.
        properties = {
                "seaweedfs.s3.endpoint=http://localhost:8333",
                "seaweedfs.s3.access-key=integration-test-access-key",
                "seaweedfs.s3.secret-key=integration-test-secret-key",
                "external-api.reactur.base-url=http://localhost:8000"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
// @Transactional wraps each test method in a real transaction that's rolled back at the end -
// without this, every real MockMvc-driven write here would actually commit to the shared, named
// H2 instance (jdbc:h2:mem:kineticsdb, see application-test.yml) and could leak into other test
// classes (e.g. @DataJpaTest repository tests asserting on unscoped, whole-table counts) depending
// on execution order/context lifecycle. Confirmed root cause of a real, order-dependent test
// failure observed in IntelliJ's runner (though not under `./mvnw -o clean test`'s default order).
@Transactional
class FolderOwnershipAndAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReactantRepository reactantRepository;

    @Autowired
    private ReactantStructureRepository reactantStructureRepository;

    @Autowired
    private SolventRepository solventRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private StructureApiClient structureApiClient;

    @MockitoBean
    private S3Client s3Client;

    /**
     * Identical in spirit and implementation to
     * {@code ExperimentLifecycleIntegrationTest#asRealJwtSubject} - see that method's Javadoc for
     * the full rationale on why this pattern (rather than the {@code jwt()} post-processor) is
     * required to exercise the real {@code CustomJwtAuthenticationConverter}.
     */
    private RequestPostProcessor asRealJwtSubject(String keycloakId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue(keycloakId)
                .header("alg", "none")
                .claim("sub", keycloakId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        when(jwtDecoder.decode(keycloakId)).thenReturn(jwt);

        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + keycloakId);
            return request;
        };
    }

    /**
     * Builds a minimal-but-valid {@link SaveExperimentDto} - shared by every scenario below that
     * needs a real experiment to own/fold/archive but doesn't otherwise care about the exact
     * metadata/measurement values.
     */
    private SaveExperimentDto buildValidExperimentDto(
            UUID reactantAId, UUID reactantBId, String scientistId, UUID solventId
    ) {
        SaveMetadataDto metadata = new SaveMetadataDto(
                LocalDate.now().minusDays(1),
                scientistId,
                20.0,
                solventId,
                1.0,
                1.0,
                null,
                null,
                null,
                "Tier-1 integration test - Folders/Authorization matrix"
        );
        MeasurementDataDto measurement = new MeasurementDataDto(null, 3.0, 0.1, false);
        return new SaveExperimentDto(reactantAId, reactantBId, metadata, measurement, List.of());
    }

    /**
     * Issues a real {@code POST /api/experiment} (creator == scientist == {@code creatorId}) and
     * returns the created experiment's UUID - shared by every scenario below that needs a real,
     * persisted experiment as a starting point.
     */
    private UUID createOwnExperiment(String creatorId) throws Exception {
        User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);
        Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                reactantRepository, reactantStructureRepository, creator, "IntReactantA-" + UUID.randomUUID());
        Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                reactantRepository, reactantStructureRepository, creator, "IntReactantB-" + UUID.randomUUID());
        var solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "IntSolvent-" + UUID.randomUUID());

        SaveExperimentDto dto = buildValidExperimentDto(reactantA.getId(), reactantB.getId(), creatorId, solvent.getId());

        String responseBody = mockMvc.perform(post("/api/experiment")
                        .with(asRealJwtSubject(creatorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, GetExperimentDto.class).uuid();
    }

    /** Issues a real {@code POST /api/folders} and returns the created folder's UUID. */
    private UUID createFolder(String ownerId, String name) throws Exception {
        String responseBody = mockMvc.perform(post("/api/folders")
                        .with(asRealJwtSubject(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveFolderDto(name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, GetFolderDto.class).id();
    }

    /**
     * Issues a real {@code GET /api/folders} for {@code userId} and returns the parsed list of
     * {@link GetFolderDto} - the "independent follow-up read" used throughout this class to prove
     * real persistence rather than trusting a mutating endpoint's own echoed response.
     */
    private List<GetFolderDto> getMyFolders(String userId) throws Exception {
        String responseBody = mockMvc.perform(get("/api/folders")
                        .with(asRealJwtSubject(userId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Arrays.asList(objectMapper.readValue(responseBody, GetFolderDto[].class));
    }

    private GetFolderDto findFolder(List<GetFolderDto> folders, UUID folderId) {
        return folders.stream().filter(f -> f.id().equals(folderId)).findFirst()
                .orElseThrow(() -> new AssertionError("Folder " + folderId + " not found in: " + folders));
    }

    // ------------------------------------------------------------------------------------------
    // INT-12 - Owner CRUD succeeds, including the one-folder-per-experiment auto-move rule
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int12FolderOwnerCrudAndAutoMove {

        /**
         * A real USER (creator+scientist of a real experiment) creates a folder, adds their
         * experiment to it, removes it, and deletes the folder - every step confirmed via an
         * independent, follow-up real {@code GET /api/folders} rather than trusting the mutating
         * endpoint's own echoed response.
         */
        @Test
        void ownerCreatesAddsRemovesAndDeletes_allSucceedWithRealPersistence() throws Exception {
            String userAId = "integration-int12-usera-" + UUID.randomUUID();
            UUID experimentId = createOwnExperiment(userAId);

            UUID folderId = createFolder(userAId, "Folder-" + UUID.randomUUID());

            // Confirm the folder itself was really persisted, with no experiments yet.
            GetFolderDto folderAfterCreate = findFolder(getMyFolders(userAId), folderId);
            assertTrue(folderAfterCreate.experimentIds().isEmpty());

            mockMvc.perform(post("/api/folders/{folderId}/experiments/{experimentId}", folderId, experimentId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.experimentIds", hasItem(experimentId.toString())));

            // Independent follow-up read: proves the addition was really persisted.
            GetFolderDto folderAfterAdd = findFolder(getMyFolders(userAId), folderId);
            assertEquals(List.of(experimentId), folderAfterAdd.experimentIds());

            mockMvc.perform(delete("/api/folders/{folderId}/experiments/{experimentId}", folderId, experimentId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.experimentIds", empty()));

            // Independent follow-up read: proves the removal was really persisted.
            GetFolderDto folderAfterRemove = findFolder(getMyFolders(userAId), folderId);
            assertTrue(folderAfterRemove.experimentIds().isEmpty());

            mockMvc.perform(delete("/api/folders/{folderId}", folderId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isNoContent());

            // Independent follow-up read: the folder itself must be gone from the list entirely.
            List<GetFolderDto> foldersAfterDelete = getMyFolders(userAId);
            assertTrue(foldersAfterDelete.stream().noneMatch(f -> f.id().equals(folderId)),
                    "Deleted folder must no longer appear in GET /api/folders: " + foldersAfterDelete);
        }

        /**
         * Sub-case surfaced by directly reading {@code ExperimentFolderServiceImpl.addExperimentToFolder}
         * (the "One Folder Per User" block):
         * <pre>
         *     // One Folder Per User: Find all user's folders and remove this experiment
         *     List&lt;ExperimentFolder&gt; allUserFolders = folderRepository.findAllByOwner_Id(requestingUserId);
         *     for (ExperimentFolder oldFolder : allUserFolders) {
         *       if (!oldFolder.getId().equals(folderId)) {
         *         boolean removed = oldFolder.getExperiments().removeIf(exp -&gt; exp.getId().equals(experimentId));
         *         ...
         * </pre>
         * (verified directly in the current source, lines 80-90). Adding an experiment already
         * present in one of the user's OTHER folders silently removes it from there first, so an
         * experiment can only ever live in one of a given user's folders at a time.
         */
        @Test
        void addingExperimentAlreadyInAnotherOwnFolder_autoMovesItAndRemovesFromTheFirst() throws Exception {
            String userAId = "integration-int12-automove-" + UUID.randomUUID();
            UUID experimentId = createOwnExperiment(userAId);

            UUID firstFolderId = createFolder(userAId, "FirstFolder-" + UUID.randomUUID());
            UUID secondFolderId = createFolder(userAId, "SecondFolder-" + UUID.randomUUID());

            mockMvc.perform(post("/api/folders/{folderId}/experiments/{experimentId}", firstFolderId, experimentId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isOk());

            // Independent follow-up read: confirm the experiment really landed in the first folder.
            GetFolderDto firstFolderBeforeMove = findFolder(getMyFolders(userAId), firstFolderId);
            assertEquals(List.of(experimentId), firstFolderBeforeMove.experimentIds());

            mockMvc.perform(post("/api/folders/{folderId}/experiments/{experimentId}", secondFolderId, experimentId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.experimentIds", hasItem(experimentId.toString())));

            // Independent follow-up read: the experiment must now be ONLY in the second folder.
            List<GetFolderDto> foldersAfterMove = getMyFolders(userAId);
            GetFolderDto firstFolderAfterMove = findFolder(foldersAfterMove, firstFolderId);
            GetFolderDto secondFolderAfterMove = findFolder(foldersAfterMove, secondFolderId);

            assertTrue(firstFolderAfterMove.experimentIds().isEmpty(),
                    "Experiment must have been auto-removed from the first folder: " + firstFolderAfterMove);
            assertEquals(List.of(experimentId), secondFolderAfterMove.experimentIds());
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-13 - Owner cannot add a non-owned/non-scientist experiment to their own folder
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int13FolderOwnerCannotAddNonOwnedExperiment {

        /**
         * {@code UserA} genuinely owns the target folder - the folder-ownership check inside
         * {@code getFolderAndValidateOwnership} passes. Rejection instead comes from a second,
         * independent check directly inside {@code addExperimentToFolder} (verified in the current
         * source):
         * <pre>
         *     if (!experimentAuthorization.isCreatorOrScientist(experiment, requestingUserId)) {
         *       log.warn("User {} tried to add non-owned experiment {} to folder", requestingUserId, experimentId);
         *       throw new AccessDeniedException("User not authorized to add a not-owned experiment to a folder");
         *     }
         * </pre>
         * (lines 75-78 in the current file). A follow-up {@code GET /api/folders} confirms the
         * folder was never actually mutated by the rejected call.
         */
        @Test
        void addingNonOwnedNonScientistExperiment_isForbiddenEvenInOwnFolder() throws Exception {
            String userAId = "integration-int13-usera-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, userAId, Role.USER);
            UUID folderId = createFolder(userAId, "Folder-" + UUID.randomUUID());

            // UserB's real experiment - UserA is neither creator nor designated scientist.
            String userBId = "integration-int13-userb-" + UUID.randomUUID();
            UUID userBExperimentId = createOwnExperiment(userBId);

            mockMvc.perform(post("/api/folders/{folderId}/experiments/{experimentId}", folderId, userBExperimentId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isForbidden());

            // Independent follow-up read: the folder must remain empty - the rejected call never mutated it.
            GetFolderDto folderAfterAttempt = findFolder(getMyFolders(userAId), folderId);
            assertTrue(folderAfterAttempt.experimentIds().isEmpty());
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-14 - Zero privileged bypass, even for ADMIN/SUPERVISOR
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int14FolderZeroPrivilegedBypass {

        /**
         * {@code UserA}'s real folder (containing a real experiment) is targeted by a real
         * {@code ADMIN} and, separately, a real {@code SUPERVISOR} - neither of whom owns it.
         * All 6 attempts (2 roles x {add, remove, delete}) must be 403, confirmed by direct read of
         * {@code ExperimentFolderServiceImpl.getFolderAndValidateOwnership} (verified in the
         * current source):
         * <pre>
         *   private ExperimentFolder getFolderAndValidateOwnership(UUID folderId, String requestingUserId) {
         *     ExperimentFolder folder = folderRepository.findById(folderId)
         *       .orElseThrow(() -&gt; new ResourceNotFoundException("Folder not found."));
         *
         *     if (!folder.getOwner().getId().equals(requestingUserId)) {
         *       ...
         *       throw new AccessDeniedException("You do not have permission to access this folder.");
         *     }
         *     return folder;
         *   }
         * </pre>
         * (lines 135-146 in the current file) - a pure {@code owner.getId().equals(requestingUserId)}
         * check with no {@code isPrivileged}/role branch at all, unlike every other privileged
         * resource in the system. This is called first by {@code addExperimentToFolder},
         * {@code removeExperimentFromFolder}, and {@code deleteFolder} alike, so it fires before
         * any experiment lookup - the target experiment doesn't even need to exist for the 403 to
         * be returned, though a real one is used below anyway for realism.
         * <p>
         * A follow-up {@code GET /api/folders} (as {@code UserA}) confirms the folder and its
         * experiment survive every rejected attempt untouched.
         */
        @Test
        void adminAndSupervisor_forbiddenOnAddRemoveAndDelete() throws Exception {
            String userAId = "integration-int14-usera-" + UUID.randomUUID();
            UUID experimentId = createOwnExperiment(userAId);
            UUID folderId = createFolder(userAId, "Folder-" + UUID.randomUUID());

            mockMvc.perform(post("/api/folders/{folderId}/experiments/{experimentId}", folderId, experimentId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isOk());

            String adminId = "integration-int14-admin-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);
            String supervisorId = "integration-int14-supervisor-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, supervisorId, Role.SUPERVISOR);

            for (String privilegedId : List.of(adminId, supervisorId)) {
                mockMvc.perform(post("/api/folders/{folderId}/experiments/{experimentId}", folderId, experimentId)
                                .with(asRealJwtSubject(privilegedId)))
                        .andExpect(status().isForbidden());

                mockMvc.perform(delete("/api/folders/{folderId}/experiments/{experimentId}", folderId, experimentId)
                                .with(asRealJwtSubject(privilegedId)))
                        .andExpect(status().isForbidden());

                mockMvc.perform(delete("/api/folders/{folderId}", folderId)
                                .with(asRealJwtSubject(privilegedId)))
                        .andExpect(status().isForbidden());
            }

            // Independent follow-up read: the folder and its experiment must have survived every
            // rejected privileged attempt completely untouched.
            GetFolderDto folderAfterAllAttempts = findFolder(getMyFolders(userAId), folderId);
            assertEquals(List.of(experimentId), folderAfterAllAttempts.experimentIds());
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-16 - Privileged bypass, the allowed direction (completes the pilot's Scenario B pair)
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int16PrivilegedBypassAllowedDirection {

        /**
         * {@code UserA} creates a real experiment (creator+scientist). A real {@code SUPERVISOR}
         * - neither creator nor scientist - archives it via {@code DELETE /api/experiment/{uuid}}.
         * Per {@code ExperimentAuthorization#canModify} (verified in the current source):
         * <pre>
         *     public boolean canModify(Experiment experiment, String userId, Role role) {
         *         return isPrivileged(role) || isCreatorOrScientist(experiment, userId);
         *     }
         * </pre>
         * and {@code isPrivileged}:
         * <pre>
         *     public boolean isPrivileged(Role role) {
         *         return role == Role.ADMIN || role == Role.SUPERVISOR;
         *     }
         * </pre>
         * this must succeed (204) - the positive counterpart to the existing pilot's
         * {@code ExperimentLifecycleIntegrationTest.scenarioB_*}, which only proved the *forbidden*
         * direction for a plain, non-creator/non-scientist {@code Role.USER}. A follow-up real
         * {@code GET} (as {@code UserA}, since the experiment is now archived and - per
         * {@code ExperimentAuthorization#canView} - only creator/scientist/privileged may view an
         * archived one) confirms {@code archivedAt} is genuinely set, not merely that the DELETE
         * call itself returned 204.
         */
        @Test
        void supervisorArchivesNonOwnedExperiment_succeedsAndArchivedAtIsSet() throws Exception {
            String userAId = "integration-int16-usera-" + UUID.randomUUID();
            UUID experimentId = createOwnExperiment(userAId);

            String supervisorId = "integration-int16-supervisor-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, supervisorId, Role.SUPERVISOR);

            mockMvc.perform(delete("/api/experiment/{uuid}", experimentId)
                            .with(asRealJwtSubject(supervisorId)))
                    .andExpect(status().isNoContent());

            // Independent follow-up read: proves the archive was really persisted, not merely that
            // the DELETE call itself returned 204.
            mockMvc.perform(get("/api/experiment/{uuid}", experimentId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.archivedAt").exists());
        }
    }
}
