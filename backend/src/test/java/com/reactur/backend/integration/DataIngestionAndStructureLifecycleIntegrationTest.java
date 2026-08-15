package com.reactur.backend.integration;

import com.reactur.backend.client.JobStatusResult;
import com.reactur.backend.client.StructureApiClient;
import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.enums.JobStatus;
import com.reactur.backend.enums.Role;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.services.StructureService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import software.amazon.awssdk.services.s3.S3Client;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier-1 integration tests covering INT-01 through INT-06 of
 * {@code backend/docs/INTEGRATION_TEST_SCENARIOS.md} - Data Ingestion & Metadata Standardization
 * (A) and 3D Structure Generation / Human-in-the-Loop (B).
 * <p>
 * Follows exactly the pattern established by {@link ExperimentLifecycleIntegrationTest}: real
 * Spring context, real H2 database, real security filter chain, real
 * {@code CustomJwtAuthenticationConverter}, real services/repositories. See that class's Javadoc
 * for the full rationale on why {@code SecurityMockMvcRequestPostProcessors.jwt()} is never used
 * here, in favor of {@link #asRealJwtSubject(String)} (a real {@code Authorization: Bearer <sub>}
 * header decoded by a stubbed {@link JwtDecoder}, routed through the real
 * {@code BearerTokenAuthenticationFilter} -> {@code JwtAuthenticationProvider} ->
 * {@code CustomJwtAuthenticationConverter} pipeline).
 * <p>
 * Only three beans are mocked, for the same reason as the pilot: {@link JwtDecoder} (no real
 * Keycloak in this tier), {@link StructureApiClient} (external chemistry structure-generation
 * API), and {@link S3Client} (the SeaweedFS/S3 file storage backend). Everything else - including
 * the real {@link StructureService} bean, autowired directly below so INT-05 can invoke its
 * {@code @Scheduled} polling method synchronously rather than waiting on real scheduling - is real.
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
class DataIngestionAndStructureLifecycleIntegrationTest {

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

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private ModalityRepository modalityRepository;

    @Autowired
    private StructureService structureService;

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
     * Builds a minimal-but-valid {@link SaveExperimentDto}, shared across several scenarios below
     * that don't otherwise care about the exact metadata/measurement values.
     */
    private SaveExperimentDto buildValidExperimentDto(
            UUID reactantAId, UUID reactantBId, String scientistId, UUID solventId, UUID instrumentId, UUID modalityId
    ) {
        SaveMetadataDto metadata = new SaveMetadataDto(
                LocalDate.now().minusDays(1),
                scientistId,
                22.0,
                solventId,
                1.0,
                1.0,
                instrumentId,
                modalityId,
                400.0,
                "Tier-1 integration test"
        );
        MeasurementDataDto measurement = new MeasurementDataDto(null, 4.4, 0.1, false);
        return new SaveExperimentDto(reactantAId, reactantBId, metadata, measurement, List.of());
    }

    // ------------------------------------------------------------------------------------------
    // INT-01 - Full round trip: experiment creation, metadata/file separation, independent re-read
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int01DataIngestionRoundTrip {

        /**
         * A real USER (creator+scientist) creates a real experiment, then POSTs a raw CSV file to
         * it via {@code ExperimentFilesEndpoint} (mounted at {@code /api/file/{experimentId}} -
         * verified by reading that class directly, NOT {@code /api/experiment/{uuid}/files} as a
         * naive guess might assume). Both the metadata and the attachment are then independently
         * re-read (two separate real GET requests, neither of which reuses the POST responses),
         * proving the metadata/file separation and real persistence of both halves.
         * <p>
         * Builds on {@code ExperimentLifecycleIntegrationTest.scenarioA_*} (which only covers the
         * metadata half) by adding the file-attachment half; the metadata-only assertions are kept
         * intentionally minimal here to avoid duplicating that coverage.
         */
        @Test
        void createExperimentThenUploadFile_bothPersistIndependently() throws Exception {
            String creatorId = "integration-int01-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int01ReactantA-" + UUID.randomUUID());
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int01ReactantB-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Int01Solvent-" + UUID.randomUUID());
            Instrument instrument = IntegrationTestHelpers.persistInstrument(instrumentRepository, "Int01Instrument-" + UUID.randomUUID());
            Modality modality = IntegrationTestHelpers.persistModality(modalityRepository, "Int01Modality-" + UUID.randomUUID());

            SaveExperimentDto requestDto = buildValidExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), instrument.getId(), modality.getId());

            String createResponseBody = mockMvc.perform(post("/api/experiment")
                            .with(asRealJwtSubject(creatorId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID experimentId = objectMapper.readValue(createResponseBody, GetExperimentDto.class).uuid();

            MockMultipartFile csvFile = new MockMultipartFile(
                    "file",
                    "measurement-data.csv",
                    "text/csv",
                    "time,signal\n0,1.23\n1,2.34\n".getBytes(StandardCharsets.UTF_8));

            // S3Client is mocked - putObject is void, so no stubbing is needed (default Mockito
            // behavior for a void method on a mock is to do nothing).
            String uploadResponseBody = mockMvc.perform(multipart("/api/file/{experimentId}", experimentId)
                            .file(csvFile)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uuid").exists())
                    .andExpect(jsonPath("$.fileName").value("measurement-data.csv"))
                    .andReturn().getResponse().getContentAsString();

            ExperimentAttachmentDetailsDto uploaded =
                    objectMapper.readValue(uploadResponseBody, ExperimentAttachmentDetailsDto.class);
            assertNotNull(uploaded.uuid(), "Uploaded attachment must have a non-null uuid");

            // Independent re-read #1: a fresh GET /api/experiment/{uuid} - not the create response -
            // shows the metadata correctly persisted AND the attachment listed separately from it.
            mockMvc.perform(get("/api/experiment/{uuid}", experimentId)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(experimentId.toString()))
                    .andExpect(jsonPath("$.metadata.temperature").value(22.0))
                    .andExpect(jsonPath("$.attachments", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.attachments[0].uuid").value(uploaded.uuid().toString()))
                    .andExpect(jsonPath("$.attachments[0].fileName").value("measurement-data.csv"));

            // Independent re-read #2: the attachment is independently retrievable via its own
            // details endpoint, entirely separate from both prior requests.
            mockMvc.perform(get("/api/file/{fileId}/details", uploaded.uuid())
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(uploaded.uuid().toString()))
                    .andExpect(jsonPath("$.fileName").value("measurement-data.csv"))
                    .andExpect(jsonPath("$.fileType").value("text/csv"));
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-02 - Mandatory metadata rejected end-to-end, nothing persisted
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int02MandatoryMetadataRejected {

        /**
         * A {@code SaveExperimentDto} missing a mandatory metadata field (a null {@code solvent},
         * which carries {@code @NotNull} on {@link SaveMetadataDto}) is rejected end-to-end with a
         * real 400 from Bean Validation - real {@code MethodArgumentNotValidException} ->
         * {@code GlobalExceptionHandler}, not a mocked/simulated rejection. A follow-up real search
         * scoped to this scientist then confirms zero experiments exist for them, proving nothing
         * was persisted despite the request reaching the controller layer.
         */
        @Test
        void missingSolvent_rejectedAndNothingPersisted() throws Exception {
            String creatorId = "integration-int02-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int02ReactantA-" + UUID.randomUUID());
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int02ReactantB-" + UUID.randomUUID());

            SaveMetadataDto metadataMissingSolvent = new SaveMetadataDto(
                    LocalDate.now().minusDays(1),
                    creatorId,
                    22.0,
                    null, // mandatory field omitted
                    1.0,
                    1.0,
                    null,
                    null,
                    null,
                    "Missing mandatory solvent"
            );
            MeasurementDataDto measurement = new MeasurementDataDto(null, 4.4, 0.1, false);
            SaveExperimentDto requestDto =
                    new SaveExperimentDto(reactantA.getId(), reactantB.getId(), metadataMissingSolvent, measurement, List.of());

            mockMvc.perform(post("/api/experiment")
                            .with(asRealJwtSubject(creatorId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());

            // Independent follow-up: search scoped to this scientist must show nothing was persisted.
            mockMvc.perform(get("/api/experiment")
                            .param("scientist", creatorId)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-03 - Reactant without an ACTIVE structure blocks experiment creation
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int03ReactantWithoutActiveStructureBlocksCreation {

        /**
         * A real Reactant with only a PENDING structure (not yet ACTIVE) is referenced as reactant
         * A in a create request. The real {@code ExperimentValidator.validateReactantHasActiveStructure}
         * path rejects it with 422 - and a follow-up search scoped to this scientist confirms
         * nothing was persisted.
         */
        @Test
        void pendingStructureOnly_rejectsExperimentCreationWith422() throws Exception {
            String creatorId = "integration-int03-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            Reactant reactantWithoutActiveStructure = IntegrationTestHelpers.persistReactantWithStructure(
                    reactantRepository, reactantStructureRepository, creator,
                    "Int03PendingReactant-" + UUID.randomUUID(), StructureFlag.PENDING);
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int03ReactantB-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Int03Solvent-" + UUID.randomUUID());

            SaveExperimentDto requestDto = buildValidExperimentDto(
                    reactantWithoutActiveStructure.getId(), reactantB.getId(), creatorId, solvent.getId(), null, null);

            mockMvc.perform(post("/api/experiment")
                            .with(asRealJwtSubject(creatorId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isUnprocessableContent());

            // Independent follow-up: nothing was persisted for this scientist.
            mockMvc.perform(get("/api/experiment")
                            .param("scientist", creatorId)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-04 - Archived-reference resilience on update
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int04ArchivedReferenceResilienceOnUpdate {

        /**
         * An experiment is created referencing a real Solvent while it is still active. An ADMIN
         * then archives that Solvent via the real {@code DELETE /api/solvent/{uuid}} endpoint
         * (soft-archive, sets {@code archivedAt} - never a hard delete). The experiment's
         * creator/scientist then issues a real {@code PUT /api/experiment/{uuid}} re-sending the
         * exact same (now-archived) solvent reference, unchanged.
         * <p>
         * This must succeed (200): confirmed directly against the actual current
         * {@code ExperimentValidator.validate()} source (shared by create+update), which resolves
         * the solvent via a plain {@code solventRepository.findById(...)} - existence only, with
         * no {@code archivedAt} check anywhere in that method or in
         * {@code OptionServiceImpl.archiveOption} (which also never deletes the row, only stamps
         * {@code archivedAt}). This is a regression guard: if {@code ExperimentValidator} ever
         * grows an {@code archivedAt} check, this test should start failing.
         */
        @Test
        void updateAfterReferencedSolventArchived_stillSucceeds() throws Exception {
            String creatorId = "integration-int04-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            String adminId = "integration-int04-admin-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);

            Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int04ReactantA-" + UUID.randomUUID());
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int04ReactantB-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Int04Solvent-" + UUID.randomUUID());

            SaveExperimentDto createDto = buildValidExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), null, null);

            String createResponseBody = mockMvc.perform(post("/api/experiment")
                            .with(asRealJwtSubject(creatorId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID experimentId = objectMapper.readValue(createResponseBody, GetExperimentDto.class).uuid();

            // A real ADMIN archives the referenced solvent - a real soft-archive, not a hard delete.
            mockMvc.perform(delete("/api/solvent/{uuid}", solvent.getId())
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isOk());

            // The creator/scientist re-sends the SAME (now-archived) solvent reference, unchanged.
            SaveExperimentDto updateDto = buildValidExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), null, null);

            mockMvc.perform(put("/api/experiment/{uuid}", experimentId)
                            .with(asRealJwtSubject(creatorId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.solvent.uuid").value(solvent.getId().toString()));

            // Independent follow-up read: a fresh GET confirms the update was genuinely persisted.
            mockMvc.perform(get("/api/experiment/{uuid}", experimentId)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.solvent.uuid").value(solvent.getId().toString()));
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-05 - Full structure lifecycle: PENDING -> READY -> ACTIVE -> usable in an experiment
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int05StructureLifecycle {

        /**
         * A real Reactant with no existing structures. An ADMIN calls
         * {@code POST /api/structure/{reactantId}/generate} (stubbed {@code triggerJob} returns a
         * canned job ID) - creating a real PENDING row. {@code StructureServiceImpl}'s
         * {@code @Scheduled} poll ({@code fetchPendingStructuresFromAPI}) is invoked directly
         * (via the real, autowired {@link StructureService} bean) rather than waiting on real
         * scheduling, with the stubbed {@code checkJobStatus} reporting success - turning the row
         * READY. The ADMIN then calls {@code POST /api/structure/{structureId}/select}, turning it
         * ACTIVE. Finally, a subsequent real experiment-creation request referencing that reactant
         * succeeds - the positive counterpart to INT-03's negative case.
         */
        @Test
        void pendingToReadyToActive_thenUsableInExperiment() throws Exception {
            String adminId = "integration-int05-admin-" + UUID.randomUUID();
            User admin = IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);

            Reactant reactant = IntegrationTestHelpers.persistReactant(
                    reactantRepository, admin, "Int05Reactant-" + UUID.randomUUID());

            String jobId = "mock-job-" + UUID.randomUUID();
            when(structureApiClient.triggerJob(anyString())).thenReturn(jobId);

            mockMvc.perform(post("/api/structure/{reactantId}/generate", reactant.getId())
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isNoContent());

            // Independent follow-up read: a real PENDING row was persisted with the returned job ID.
            List<ReactantStructure> afterGenerate = reactantStructureRepository.findByReactant_Id(reactant.getId());
            assertEquals(1, afterGenerate.size());
            assertEquals(StructureFlag.PENDING, afterGenerate.get(0).getFlag());
            assertEquals(jobId, afterGenerate.get(0).getExternalJobId());

            when(structureApiClient.checkJobStatus(jobId))
                    .thenReturn(new JobStatusResult(JobStatus.SUCCEEDED, List.of("mock-xyz-structure-payload"), null));

            // Invoke the @Scheduled poll method directly, synchronously, on the real bean - rather
            // than waiting on the real fixedDelay=60000 scheduling.
            structureService.fetchPendingStructuresFromAPI();

            // Independent follow-up read: the row has genuinely transitioned to READY in the DB.
            List<ReactantStructure> afterPoll = reactantStructureRepository.findByReactant_Id(reactant.getId());
            assertEquals(1, afterPoll.size());
            assertEquals(StructureFlag.READY, afterPoll.get(0).getFlag());
            assertEquals("mock-xyz-structure-payload", afterPoll.get(0).getStructure());
            UUID readyStructureId = afterPoll.get(0).getId();

            mockMvc.perform(post("/api/structure/{structureId}/select", readyStructureId)
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.flag").value(StructureFlag.ACTIVE.name()));

            // Independent follow-up read: a fresh repository query confirms the real ACTIVE state.
            ReactantStructure activeStructure = reactantStructureRepository.findById(readyStructureId).orElseThrow();
            assertEquals(StructureFlag.ACTIVE, activeStructure.getFlag());

            // Positive counterpart to INT-03: this reactant is now usable in a new experiment.
            Reactant otherReactant = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, admin, "Int05OtherReactant-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Int05Solvent-" + UUID.randomUUID());

            SaveExperimentDto experimentDto = buildValidExperimentDto(
                    reactant.getId(), otherReactant.getId(), adminId, solvent.getId(), null, null);

            mockMvc.perform(post("/api/experiment")
                            .with(asRealJwtSubject(adminId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(experimentDto)))
                    .andExpect(status().isCreated());
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-06 - Structure endpoints are ADMIN/SUPERVISOR-only
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int06StructureEndpointsRoleGate {

        /**
         * A plain USER is forbidden (403) on all three {@code /api/structure/**} endpoints -
         * {@code uploadStructure}, {@code generateStructureFromAPI}, {@code selectStructure} (exact
         * paths/methods confirmed by reading {@code StructureEndpoint.java} directly). Since
         * {@code @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")} is evaluated before the
         * controller method body runs, arbitrary (non-persisted) path-variable UUIDs are sufficient
         * to prove the role gate - the request never reaches the service/DB layer at all.
         */
        @Test
        void plainUser_forbiddenOnAllThreeStructureEndpoints() throws Exception {
            String userId = "integration-int06-user-" + UUID.randomUUID();
            User user = IntegrationTestHelpers.persistUser(userRepository, userId, Role.USER);

            Reactant reactant = IntegrationTestHelpers.persistReactant(
                    reactantRepository, user, "Int06Reactant-" + UUID.randomUUID());

            MockMultipartFile structureFile = new MockMultipartFile(
                    "file", "structure.mol", "chemical/x-mdl-molfile", "mock-structure-payload".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/structure/{reactantId}", reactant.getId())
                            .file(structureFile)
                            .with(asRealJwtSubject(userId)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/structure/{reactantId}/generate", reactant.getId())
                            .with(asRealJwtSubject(userId)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/structure/{structureId}/select", UUID.randomUUID())
                            .with(asRealJwtSubject(userId)))
                    .andExpect(status().isForbidden());
        }

        /**
         * Both ADMIN and SUPERVISOR succeed on all three {@code /api/structure/**} endpoints,
         * proving parity per the settled authorization matrix (see
         * {@code backend/docs/AUTHORIZATION_POLICY.md}) - this is the positive counterpart to the
         * forbidden-USER case above.
         */
        @Test
        void adminAndSupervisor_succeedOnAllThreeStructureEndpoints() throws Exception {
            assertAllThreeStructureEndpointsSucceedFor(Role.ADMIN, "int06-admin");
            assertAllThreeStructureEndpointsSucceedFor(Role.SUPERVISOR, "int06-supervisor");
        }

        private void assertAllThreeStructureEndpointsSucceedFor(Role role, String subjectPrefix) throws Exception {
            String userId = "integration-" + subjectPrefix + "-" + UUID.randomUUID();
            User user = IntegrationTestHelpers.persistUser(userRepository, userId, role);

            // 1. uploadStructure
            Reactant uploadReactant = IntegrationTestHelpers.persistReactant(
                    reactantRepository, user, "Int06UploadReactant-" + subjectPrefix + "-" + UUID.randomUUID());
            MockMultipartFile structureFile = new MockMultipartFile(
                    "file", "structure.mol", "chemical/x-mdl-molfile", "mock-structure-payload".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/structure/{reactantId}", uploadReactant.getId())
                            .file(structureFile)
                            .with(asRealJwtSubject(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.flag").value(StructureFlag.ACTIVE.name()));

            // 2. generateStructureFromAPI
            Reactant generateReactant = IntegrationTestHelpers.persistReactant(
                    reactantRepository, user, "Int06GenerateReactant-" + subjectPrefix + "-" + UUID.randomUUID());
            String jobId = "mock-job-" + subjectPrefix + "-" + UUID.randomUUID();
            when(structureApiClient.triggerJob(anyString())).thenReturn(jobId);

            mockMvc.perform(post("/api/structure/{reactantId}/generate", generateReactant.getId())
                            .with(asRealJwtSubject(userId)))
                    .andExpect(status().isNoContent());

            // Independent follow-up read proving real persistence of the generate call.
            List<ReactantStructure> generated = reactantStructureRepository.findByReactant_Id(generateReactant.getId());
            assertEquals(1, generated.size());
            assertEquals(StructureFlag.PENDING, generated.get(0).getFlag());

            // 3. selectStructure
            Reactant selectReactant = IntegrationTestHelpers.persistReactantWithStructure(
                    reactantRepository, reactantStructureRepository, user,
                    "Int06SelectReactant-" + subjectPrefix + "-" + UUID.randomUUID(), StructureFlag.READY);
            UUID readyStructureId = reactantStructureRepository.findByReactant_Id(selectReactant.getId())
                    .get(0).getId();

            mockMvc.perform(post("/api/structure/{structureId}/select", readyStructureId)
                            .with(asRealJwtSubject(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.flag").value(StructureFlag.ACTIVE.name()));

            // Independent follow-up read proving real persistence of the select call.
            ReactantStructure selected = reactantStructureRepository.findById(readyStructureId).orElseThrow();
            assertEquals(StructureFlag.ACTIVE, selected.getFlag());
        }
    }
}
