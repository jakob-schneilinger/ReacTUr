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
 * Tier-1 integration tests covering Folder Ownership and the remainder of the Authorization Matrix
 * scenarios (SCENARIO-12 through SCENARIO-16).
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
 * <b>SCENARIO-15 (the "UNAUTHORIZED global gate" scenario) is intentionally NOT reimplemented here:</b>
 * it is already fully covered by
 * {@link ExperimentLifecycleIntegrationTest#scenarioC_realConverterDbRoleLookup_determinesAuthorizationOutcome()},
 * which proves a real {@code Role.UNAUTHORIZED} DB row is rejected by {@code SecurityConfig}'s
 * global gate. Nothing about that gate is Folder-specific, so duplicating it here would add no
 * genuine coverage.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "seaweedfs.s3.endpoint=http://localhost:8333",
                "seaweedfs.s3.access-key=integration-test-access-key",
                "seaweedfs.s3.secret-key=integration-test-secret-key",
                "external-api.reactur.base-url=http://localhost:8000"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

    // Identical in spirit and implementation to ExperimentLifecycleIntegrationTest.asRealJwtSubject.
    // See that method's comment for the full rationale on why this pattern (rather than the jwt()
    // post-processor) is required to exercise the real CustomJwtAuthenticationConverter.
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

    // Builds a minimal-but-valid SaveExperimentDto, shared by every scenario below that needs a real
    // experiment to own/fold/archive but doesn't otherwise care about the exact metadata/measurement
    // values.
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

    // Issues a real POST /api/experiment (creator == scientist == creatorId) and returns the
    // created experiment's UUID, shared by every scenario below that needs a real, persisted
    // experiment as a starting point.
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

    // Issues a real POST /api/folders and returns the created folder's UUID.
    private UUID createFolder(String ownerId, String name) throws Exception {
        String responseBody = mockMvc.perform(post("/api/folders")
                        .with(asRealJwtSubject(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveFolderDto(name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, GetFolderDto.class).id();
    }

    // Issues a real GET /api/folders for userId and returns the parsed list of GetFolderDto: the
    // "independent follow-up read" used throughout this class to prove real persistence rather than
    // trusting a mutating endpoint's own echoed response.
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
    // SCENARIO-12 - Owner CRUD succeeds, including the one-folder-per-experiment auto-move rule
    // ------------------------------------------------------------------------------------------
    @Nested
    class Scenario12FolderOwnerCrudAndAutoMove {

        // A user (creator and scientist of an experiment) creates a folder, adds their experiment
        // to it, removes it, and deletes the folder. Each step is checked with a follow-up GET
        // request rather than trusting what the mutating endpoint echoes back.
        @Test
        void ownerCreatesAddsRemovesAndDeletes_allSucceedWithRealPersistence() throws Exception {
            String userAId = "integration-scenario12-usera-" + UUID.randomUUID();
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

        // An experiment already sitting in one of the user's other folders is moved rather than
        // duplicated: adding it to a new folder removes it from the old one first, so it only ever
        // lives in one folder at a time.
        @Test
        void addingExperimentAlreadyInAnotherOwnFolder_autoMovesItAndRemovesFromTheFirst() throws Exception {
            String userAId = "integration-scenario12-automove-" + UUID.randomUUID();
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
    // SCENARIO-13 - Owner cannot add a non-owned/non-scientist experiment to their own folder
    // ------------------------------------------------------------------------------------------
    @Nested
    class Scenario13FolderOwnerCannotAddNonOwnedExperiment {

        // UserA owns the target folder, so ownership isn't the problem here. The rejection instead
        // comes from a separate check: only the experiment's creator or designated scientist may add
        // it to a folder. A follow-up GET confirms the folder was never actually changed by the
        // rejected call.
        @Test
        void addingNonOwnedNonScientistExperiment_isForbiddenEvenInOwnFolder() throws Exception {
            String userAId = "integration-scenario13-usera-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, userAId, Role.USER);
            UUID folderId = createFolder(userAId, "Folder-" + UUID.randomUUID());

            // UserB's real experiment. UserA is neither creator nor designated scientist.
            String userBId = "integration-scenario13-userb-" + UUID.randomUUID();
            UUID userBExperimentId = createOwnExperiment(userBId);

            mockMvc.perform(post("/api/folders/{folderId}/experiments/{experimentId}", folderId, userBExperimentId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isForbidden());

            // Independent follow-up read: the folder must remain empty, since the rejected call
            // never mutated it.
            GetFolderDto folderAfterAttempt = findFolder(getMyFolders(userAId), folderId);
            assertTrue(folderAfterAttempt.experimentIds().isEmpty());
        }
    }

    // ------------------------------------------------------------------------------------------
    // SCENARIO-14 - Zero privileged bypass, even for ADMIN/SUPERVISOR
    // ------------------------------------------------------------------------------------------
    @Nested
    class Scenario14FolderZeroPrivilegedBypass {

        // UserA's folder is targeted by an ADMIN and, separately, a SUPERVISOR, neither of whom
        // owns it. All 6 attempts (2 roles x add/remove/delete) should be 403, since folder access
        // is a pure ownership check with no privileged-role exception, unlike most other resources
        // in this system. That check runs before the folder is even touched, so the target
        // experiment doesn't strictly need to exist for the 403, though a real one is used here
        // anyway.
        //
        // A follow-up GET (as UserA) confirms the folder and its experiment survive every rejected
        // attempt untouched.
        @Test
        void adminAndSupervisor_forbiddenOnAddRemoveAndDelete() throws Exception {
            String userAId = "integration-scenario14-usera-" + UUID.randomUUID();
            UUID experimentId = createOwnExperiment(userAId);
            UUID folderId = createFolder(userAId, "Folder-" + UUID.randomUUID());

            mockMvc.perform(post("/api/folders/{folderId}/experiments/{experimentId}", folderId, experimentId)
                            .with(asRealJwtSubject(userAId)))
                    .andExpect(status().isOk());

            String adminId = "integration-scenario14-admin-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);
            String supervisorId = "integration-scenario14-supervisor-" + UUID.randomUUID();
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
    // SCENARIO-16 - Privileged bypass, the allowed direction (completes the pilot's Scenario B pair)
    // ------------------------------------------------------------------------------------------
    @Nested
    class Scenario16PrivilegedBypassAllowedDirection {

        // UserA creates an experiment (creator and scientist). A SUPERVISOR, who is neither,
        // archives it. Privileged roles (ADMIN/SUPERVISOR) can modify any experiment regardless of
        // ownership, so this should succeed (204). This is the positive counterpart to
        // ExperimentLifecycleIntegrationTest's scenario B, which only tested the forbidden direction
        // for a plain user. A follow-up GET, made as UserA since only the creator, scientist, or a
        // privileged role can view an archived experiment, confirms archivedAt is genuinely set.
        @Test
        void supervisorArchivesNonOwnedExperiment_succeedsAndArchivedAtIsSet() throws Exception {
            String userAId = "integration-scenario16-usera-" + UUID.randomUUID();
            UUID experimentId = createOwnExperiment(userAId);

            String supervisorId = "integration-scenario16-supervisor-" + UUID.randomUUID();
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
