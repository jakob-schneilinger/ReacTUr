package com.reactur.backend.integration;

import com.reactur.backend.client.StructureApiClient;
import com.reactur.backend.dtos.experiments.ExperimentAttachmentDetailsDto;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.dtos.reactants.CreateReactantDto;
import com.reactur.backend.dtos.reactants.ReactantDetailDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.options.ChemicalType;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.enums.Role;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.ChemicalTypeRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier-1 integration tests covering INT-07 through INT-11 of
 * {@code backend/docs/INTEGRATION_TEST_SCENARIOS.md} - Reactant &amp; Options Catalog Management
 * (C) and Search &amp; Retrieval (D).
 * <p>
 * Follows exactly the pattern established by {@link ExperimentLifecycleIntegrationTest} and
 * {@link DataIngestionAndStructureLifecycleIntegrationTest}: real Spring context, real H2
 * database, real security filter chain, real {@code CustomJwtAuthenticationConverter}, real
 * services/repositories. See {@link ExperimentLifecycleIntegrationTest}'s class Javadoc for the
 * full rationale on why {@code SecurityMockMvcRequestPostProcessors.jwt()} is never used here, in
 * favor of {@link #asRealJwtSubject(String)}.
 * <p>
 * Only three beans are mocked, for the same reason as the pilot: {@link JwtDecoder} (no real
 * Keycloak in this tier), {@link StructureApiClient} (external chemistry structure-generation
 * API, unused by these particular scenarios but still mocked for context-consistency with the
 * other classes in this tier), and {@link S3Client} (the SeaweedFS/S3 file storage backend).
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
class ReactantOptionsSearchAndFilesIntegrationTest {

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
    private ChemicalTypeRepository chemicalTypeRepository;

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
     * Builds a minimal-but-valid {@link SaveExperimentDto} with a caller-supplied temperature and
     * rate constant, shared by INT-10 (which needs distinct values to filter on) and INT-11 (which
     * doesn't care about the exact values).
     */
    private SaveExperimentDto buildExperimentDto(
            UUID reactantAId, UUID reactantBId, String scientistId, UUID solventId,
            double temperature, double rateConstant
    ) {
        SaveMetadataDto metadata = new SaveMetadataDto(
                LocalDate.now().minusDays(1),
                scientistId,
                temperature,
                solventId,
                1.0,
                1.0,
                null,
                null,
                null,
                "Tier-1 integration test"
        );
        MeasurementDataDto measurement = new MeasurementDataDto(null, rateConstant, 0.1, false);
        return new SaveExperimentDto(reactantAId, reactantBId, metadata, measurement, List.of());
    }

    /**
     * Issues a real {@code POST /api/experiment} and returns the created experiment's UUID -
     * shared by INT-10 and INT-11, which both need to seed one or more real experiments before
     * exercising their respective scenario.
     */
    private UUID createExperiment(String creatorId, SaveExperimentDto dto) throws Exception {
        String responseBody = mockMvc.perform(post("/api/experiment")
                        .with(asRealJwtSubject(creatorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, GetExperimentDto.class).uuid();
    }

    // ------------------------------------------------------------------------------------------
    // INT-07 - Reactant creation, required + optional fields
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int07ReactantCreation {

        /**
         * A real ADMIN POSTs a {@code CreateReactantDto} carrying only the required fields
         * ({@code smiles}, {@code chemicalType}, {@code shortName}) - every optional field is
         * {@code null}. {@code POST /api/reactant} is {@code @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")}
         * (verified directly in {@code ReactantEndpoint.java}), so ADMIN succeeds (201). An
         * independent real {@code GET /api/reactant/{uuid}} then confirms every submitted field
         * round-tripped, and that the optional fields default to empty string/empty list (per
         * {@code ReactantServiceImpl#createReactant}'s null-coalescing), never {@code null}.
         * <p>
         * Asserts {@code chemicalType.name} rather than {@code chemicalType.uuid}: per
         * {@code ReactantMapperTest}'s own documented "Known limitation", {@code ReactantMapper}
         * has no wired-up {@code ChemicalTypeMapper} for the nested entity-&gt;DTO conversion, so
         * {@code chemicalType.uuid} always comes out {@code null} even though the real persisted
         * entity has one - only {@code chemicalType.name} genuinely round-trips today.
         */
        @Test
        void requiredFieldsOnly_createsAndRoundTrips() throws Exception {
            String adminId = "integration-int07-admin-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);

            ChemicalType chemicalType = IntegrationTestHelpers.persistChemicalType(
                    chemicalTypeRepository, "Int07ChemType-" + UUID.randomUUID());

            String shortName = "Int07Required-" + UUID.randomUUID();
            CreateReactantDto requestDto = new CreateReactantDto(
                    "C1=CC=CC=C1", chemicalType.getId(), shortName,
                    null, null, null, null, null, null);

            String responseBody = mockMvc.perform(post("/api/reactant")
                            .with(asRealJwtSubject(adminId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uuid").exists())
                    .andReturn().getResponse().getContentAsString();

            ReactantDetailDto created = objectMapper.readValue(responseBody, ReactantDetailDto.class);

            // Independent follow-up read: a fresh GET - not the create response - proves real
            // persistence and shows every field correctly round-tripped.
            mockMvc.perform(get("/api/reactant/{uuid}", created.uuid())
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(created.uuid().toString()))
                    .andExpect(jsonPath("$.smiles").value("C1=CC=CC=C1"))
                    .andExpect(jsonPath("$.shortName").value(shortName))
                    .andExpect(jsonPath("$.chemicalType.name").value(chemicalType.getName()))
                    .andExpect(jsonPath("$.additionalShortNames", empty()))
                    .andExpect(jsonPath("$.chemicalName").value(""))
                    .andExpect(jsonPath("$.casNumber").value(""))
                    .andExpect(jsonPath("$.inChiKey").value(""))
                    .andExpect(jsonPath("$.comment").value(""))
                    .andExpect(jsonPath("$.references", empty()))
                    .andExpect(jsonPath("$.archivedAt").doesNotExist());
        }

        /**
         * A real SUPERVISOR (also permitted to create reactants, per the settled matrix) POSTs a
         * {@code CreateReactantDto} with every optional field populated (additional short names,
         * chemical name, CAS number, InChIKey, comment, references). An independent real GET then
         * confirms all of them - not just the required trio - round-tripped correctly.
         */
        @Test
        void allOptionalFieldsPopulated_createsAndRoundTrips() throws Exception {
            String supervisorId = "integration-int07-supervisor-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, supervisorId, Role.SUPERVISOR);

            ChemicalType chemicalType = IntegrationTestHelpers.persistChemicalType(
                    chemicalTypeRepository, "Int07ChemTypeOpt-" + UUID.randomUUID());

            String shortName = "Int07Optional-" + UUID.randomUUID();
            CreateReactantDto requestDto = new CreateReactantDto(
                    "C1=CC=CC=C1O", chemicalType.getId(), shortName,
                    List.of("Alt1-" + UUID.randomUUID(), "Alt2-" + UUID.randomUUID()),
                    "Full Chemical Name",
                    "123-45-6",
                    "ABCDEFGHIJKLMN-OPQRSTUVWX-Y",
                    "Test comment for optional fields",
                    List.of("10.1000/int07-optional-ref"));

            String responseBody = mockMvc.perform(post("/api/reactant")
                            .with(asRealJwtSubject(supervisorId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uuid").exists())
                    .andReturn().getResponse().getContentAsString();

            ReactantDetailDto created = objectMapper.readValue(responseBody, ReactantDetailDto.class);

            // Independent follow-up read.
            mockMvc.perform(get("/api/reactant/{uuid}", created.uuid())
                            .with(asRealJwtSubject(supervisorId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(created.uuid().toString()))
                    .andExpect(jsonPath("$.smiles").value("C1=CC=CC=C1O"))
                    .andExpect(jsonPath("$.shortName").value(shortName))
                    .andExpect(jsonPath("$.chemicalType.name").value(chemicalType.getName()))
                    .andExpect(jsonPath("$.additionalShortNames",
                            containsInAnyOrder(requestDto.additionalShortNames().toArray())))
                    .andExpect(jsonPath("$.chemicalName").value("Full Chemical Name"))
                    .andExpect(jsonPath("$.casNumber").value("123-45-6"))
                    .andExpect(jsonPath("$.inChiKey").value("ABCDEFGHIJKLMN-OPQRSTUVWX-Y"))
                    .andExpect(jsonPath("$.comment").value("Test comment for optional fields"))
                    .andExpect(jsonPath("$.references[0]").value("10.1000/int07-optional-ref"))
                    .andExpect(jsonPath("$.archivedAt").doesNotExist());
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-08 - Options catalog creation: ADMIN succeeds, SUPERVISOR forbidden
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int08OptionsCatalogRoleGate {

        /**
         * Representative of the 4 genuinely-identical options endpoints (chemical type, solvent,
         * instrument, modality) - {@code POST /api/chemical_type} is
         * {@code @PreAuthorize("hasRole('ADMIN')")} (verified directly in
         * {@code AbstractOptionsEndpoint.java}, the shared superclass all 4 endpoints extend
         * without overriding this annotation). A real ADMIN succeeds (201); an independent real
         * {@code GET /api/chemical_type} then confirms the new option is genuinely listed.
         */
        @Test
        void adminCreatesChemicalType_succeeds() throws Exception {
            String adminId = "integration-int08-admin-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);

            String typeName = "Int08ChemType-" + UUID.randomUUID();

            mockMvc.perform(post("/api/chemical_type")
                            .with(asRealJwtSubject(adminId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + typeName + "\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItem(typeName)));

            // Independent follow-up read: a fresh GET - not the create response - confirms real
            // persistence.
            mockMvc.perform(get("/api/chemical_type")
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", hasItem(typeName)));
        }

        /**
         * The same endpoint, called by a real SUPERVISOR - who has no elevated rights on the
         * options catalog at all, unlike Reactant creation (INT-07) - must be rejected with 403,
         * per {@code hasRole('ADMIN')} (note: not {@code hasAnyRole('ADMIN','SUPERVISOR')}, unlike
         * {@code ReactantEndpoint#createReactant}). An independent follow-up GET then confirms
         * nothing was persisted.
         */
        @Test
        void supervisorCreatesChemicalType_isForbidden() throws Exception {
            String supervisorId = "integration-int08-supervisor-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, supervisorId, Role.SUPERVISOR);

            String typeName = "Int08ForbiddenChemType-" + UUID.randomUUID();

            mockMvc.perform(post("/api/chemical_type")
                            .with(asRealJwtSubject(supervisorId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + typeName + "\"}"))
                    .andExpect(status().isForbidden());

            // Independent follow-up read: confirm it was never persisted.
            mockMvc.perform(get("/api/chemical_type")
                            .with(asRealJwtSubject(supervisorId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", not(hasItem(typeName))));
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-09 - Reactant archive: ADMIN succeeds, SUPERVISOR forbidden
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int09ReactantArchiveRoleGate {

        /**
         * {@code DELETE /api/reactant/{uuid}} is {@code @PreAuthorize("hasRole('ADMIN')")}
         * (verified directly in {@code ReactantEndpoint.java}) and returns 204. A real ADMIN
         * archives a real, experiment-free Reactant; an independent follow-up
         * {@code GET /api/reactant/{uuid}} then confirms {@code archivedAt} is now set.
         */
        @Test
        void adminArchivesReactant_succeedsAndArchivedAtSet() throws Exception {
            String adminId = "integration-int09-admin-" + UUID.randomUUID();
            User admin = IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);

            Reactant reactant = IntegrationTestHelpers.persistReactant(
                    reactantRepository, admin, "Int09AdminReactant-" + UUID.randomUUID());

            mockMvc.perform(delete("/api/reactant/{uuid}", reactant.getId())
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isNoContent());

            // Independent follow-up read: a fresh GET shows archivedAt now set.
            mockMvc.perform(get("/api/reactant/{uuid}", reactant.getId())
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.archivedAt").exists());
        }

        /**
         * The same endpoint, called by a real SUPERVISOR - who, unlike for Reactant *creation*
         * (INT-07), has no archive rights at all (archiving is treated as archive-management,
         * ADMIN-only per {@code ReactantEndpoint.java}'s comment on this exact endpoint) - must be
         * rejected with 403. An independent follow-up GET confirms the entity remains unarchived.
         */
        @Test
        void supervisorArchivesReactant_isForbiddenAndRemainsUnarchived() throws Exception {
            String supervisorId = "integration-int09-supervisor-" + UUID.randomUUID();
            User supervisor = IntegrationTestHelpers.persistUser(userRepository, supervisorId, Role.SUPERVISOR);

            Reactant reactant = IntegrationTestHelpers.persistReactant(
                    reactantRepository, supervisor, "Int09SupervisorReactant-" + UUID.randomUUID());

            mockMvc.perform(delete("/api/reactant/{uuid}", reactant.getId())
                            .with(asRealJwtSubject(supervisorId)))
                    .andExpect(status().isForbidden());

            // Independent follow-up read: a fresh GET confirms the DB row was never mutated.
            mockMvc.perform(get("/api/reactant/{uuid}", reactant.getId())
                            .with(asRealJwtSubject(supervisorId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.archivedAt").doesNotExist());
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-10 - Metadata + rate-constant-range filtering, full real round trip
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int10MetadataAndRateConstantFiltering {

        /**
         * Three real experiments are seeded for the same (uniquely random) scientist: one matching
         * both a {@code temperatureMin} filter and a nested {@code measurementSearch.rateConstantMin}
         * filter, one failing only the rate-constant filter, and one failing only the temperature
         * filter (confirmed against {@code ExperimentServiceImpl#buildSearchSpecification}, which
         * AND-combines every supplied filter). A single real
         * {@code GET /api/experiment?temperatureMin=...&measurementSearch.rateConstantMin=...}
         * (the nested dotted-path query-param binding syntax confirmed working in
         * {@code ExperimentEndpointTest}) - additionally scoped by {@code scientist} to this test's
         * unique creator, so concurrently-seeded data from other tests/methods sharing the same H2
         * instance can never leak in - returns exactly the one matching experiment, proving the
         * full controller -&gt; real Spring converter -&gt; real service -&gt; real repository/
         * Specification wiring works end-to-end over real HTTP.
         */
        @Test
        void combinedTemperatureAndRateConstantFilter_returnsOnlyMatchingExperiment() throws Exception {
            String creatorId = "integration-int10-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int10ReactantA-" + UUID.randomUUID());
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int10ReactantB-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Int10Solvent-" + UUID.randomUUID());

            // Matches both filters: temperature (25.0) >= 20.0 AND rateConstant (10.0) >= 5.0.
            UUID matchingId = createExperiment(creatorId, buildExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), 25.0, 10.0));
            // Fails the rate-constant filter only: rateConstant (2.0) < 5.0.
            createExperiment(creatorId, buildExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), 25.0, 2.0));
            // Fails the temperature filter only: temperature (10.0) < 20.0.
            createExperiment(creatorId, buildExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), 10.0, 10.0));

            mockMvc.perform(get("/api/experiment")
                            .param("scientist", creatorId)
                            .param("temperatureMin", "20.0")
                            .param("measurementSearch.rateConstantMin", "5.0")
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].uuid").value(matchingId.toString()));
        }
    }

    // ------------------------------------------------------------------------------------------
    // INT-11 - Raw file retrieval round trip
    // ------------------------------------------------------------------------------------------
    @Nested
    class Int11RawFileRetrieval {

        private static final byte[] CANNED_BYTES = "time,signal\n0,1.0\n".getBytes(StandardCharsets.UTF_8);

        /**
         * A real creator+scientist USER creates a real experiment and uploads a real file to it
         * (reusing the INT-01 pattern). The mocked {@link S3Client#getObject(GetObjectRequest)} is
         * then stubbed to return a canned byte stream, and the creator downloads that file via
         * {@code GET /api/file/{fileId}} ({@code ExperimentFilesEndpoint}). The response body bytes
         * are asserted to exactly match the canned bytes, and the {@code Content-Disposition}
         * header is confirmed to be an attachment.
         */
        @Test
        void creatorDownloadsFile_bytesMatchCannedS3Response() throws Exception {
            String creatorId = "integration-int11-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int11ReactantA-" + UUID.randomUUID());
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int11ReactantB-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Int11Solvent-" + UUID.randomUUID());

            UUID experimentId = createExperiment(creatorId, buildExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), 22.0, 4.0));

            MockMultipartFile csvFile = new MockMultipartFile(
                    "file", "int11-measurement.csv", "text/csv",
                    "time,signal\n0,1.23\n".getBytes(StandardCharsets.UTF_8));

            String uploadResponseBody = mockMvc.perform(multipart("/api/file/{experimentId}", experimentId)
                            .file(csvFile)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID fileId = objectMapper.readValue(uploadResponseBody, ExperimentAttachmentDetailsDto.class).uuid();

            when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(
                    new ResponseInputStream<>(
                            GetObjectResponse.builder().build(),
                            AbortableInputStream.create(new ByteArrayInputStream(CANNED_BYTES))));

            byte[] downloadedBytes = mockMvc.perform(get("/api/file/{fileId}", fileId)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                    .andReturn().getResponse().getContentAsByteArray();

            assertArrayEquals(CANNED_BYTES, downloadedBytes);
        }

        /**
         * Deviation from the scenario doc's literal wording, and why: per the real
         * {@code ExperimentAuthorization#canView} (read directly - not assumed), <b>any</b> approved
         * role (including a plain, non-creator/non-scientist USER) may view an <b>active</b>
         * experiment's files; only an <b>archived</b> experiment restricts viewing to the creator,
         * the designated scientist, or a privileged role. So a non-creator/non-scientist/
         * non-privileged USER downloading a file from an otherwise-untouched active experiment
         * would actually succeed (200), not 403 - the scenario doc's assumption doesn't match this
         * branch of the real code as written.
         * <p>
         * To genuinely exercise the forbidden branch the scenario intends to cover, the experiment
         * is archived first by its own creator (a real, allowed {@code DELETE
         * /api/experiment/{uuid}}, since {@code canModify} permits the creator/scientist regardless
         * of archived state) - only then does a second, real, non-creator/non-scientist/
         * non-privileged {@code USER} get a genuine 403 on the download, proving
         * {@code ExperimentAuthorization}'s ownership+role check holds through a real end-to-end
         * HTTP request in this tier, not merely at the unit level.
         */
        @Test
        void nonPrivilegedNonCreatorUser_forbiddenOnArchivedExperimentDownload() throws Exception {
            String creatorId = "integration-int11b-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            String otherUserId = "integration-int11b-other-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, otherUserId, Role.USER);

            Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int11bReactantA-" + UUID.randomUUID());
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Int11bReactantB-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Int11bSolvent-" + UUID.randomUUID());

            UUID experimentId = createExperiment(creatorId, buildExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), 22.0, 4.0));

            MockMultipartFile csvFile = new MockMultipartFile(
                    "file", "int11b-measurement.csv", "text/csv",
                    "time,signal\n0,9.99\n".getBytes(StandardCharsets.UTF_8));

            String uploadResponseBody = mockMvc.perform(multipart("/api/file/{experimentId}", experimentId)
                            .file(csvFile)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID fileId = objectMapper.readValue(uploadResponseBody, ExperimentAttachmentDetailsDto.class).uuid();

            // The creator archives their own experiment - a real, allowed DELETE.
            mockMvc.perform(delete("/api/experiment/{uuid}", experimentId)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isNoContent());

            // otherUser is neither the creator, the scientist, nor privileged - and the experiment
            // is now archived, so ExperimentAuthorization#canView genuinely denies them.
            mockMvc.perform(get("/api/file/{fileId}", fileId)
                            .with(asRealJwtSubject(otherUserId)))
                    .andExpect(status().isForbidden());
        }
    }
}
