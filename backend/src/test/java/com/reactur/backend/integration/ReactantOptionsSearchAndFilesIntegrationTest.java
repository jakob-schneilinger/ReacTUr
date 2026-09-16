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
import org.springframework.transaction.annotation.Transactional;
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
 * Tier-1 integration tests covering Reactant &amp; Options Catalog Management and Search &amp;
 * Retrieval scenarios (SCENARIO-07 through SCENARIO-11).
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
        properties = {
                "seaweedfs.s3.endpoint=http://localhost:8333",
                "seaweedfs.s3.access-key=integration-test-access-key",
                "seaweedfs.s3.secret-key=integration-test-secret-key",
                "external-api.reactur.base-url=http://localhost:8000"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
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

    // Builds a minimal-but-valid SaveExperimentDto with a caller-supplied temperature and rate
    // constant, shared by SCENARIO-10 (which needs distinct values to filter on) and SCENARIO-11 (which
    // doesn't care about the exact values).
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

    // Issues a real POST /api/experiment and returns the created experiment's UUID, shared by
    // SCENARIO-10 and SCENARIO-11, which both need to seed one or more real experiments before exercising
    // their respective scenario.
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
    // SCENARIO-07 - Reactant creation, required + optional fields
    // ------------------------------------------------------------------------------------------
    @Nested
    class Scenario07ReactantCreation {

        // An ADMIN creates a reactant with only the required fields (smiles, chemicalType,
        // shortName), leaving every optional field null. Both ADMIN and SUPERVISOR are allowed to
        // create reactants, so this succeeds (201). A follow-up GET confirms every submitted field
        // round-tripped, and that the optional fields come back as empty string/empty list rather
        // than null.
        @Test
        void requiredFieldsOnly_createsAndRoundTrips() throws Exception {
            String adminId = "integration-scenario07-admin-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);

            ChemicalType chemicalType = IntegrationTestHelpers.persistChemicalType(
                    chemicalTypeRepository, "Scenario07ChemType-" + UUID.randomUUID());

            String shortName = "Scenario07Required-" + UUID.randomUUID();
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

            // Independent follow-up read: a fresh GET, not the create response, proves real
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

        // A real SUPERVISOR (also permitted to create reactants, per the settled matrix) POSTs a
        // CreateReactantDto with every optional field populated (additional short names, chemical
        // name, CAS number, InChIKey, comment, references). An independent real GET then confirms
        // all of them, not just the required trio, round-tripped correctly.
        @Test
        void allOptionalFieldsPopulated_createsAndRoundTrips() throws Exception {
            String supervisorId = "integration-scenario07-supervisor-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, supervisorId, Role.SUPERVISOR);

            ChemicalType chemicalType = IntegrationTestHelpers.persistChemicalType(
                    chemicalTypeRepository, "Scenario07ChemTypeOpt-" + UUID.randomUUID());

            String shortName = "Scenario07Optional-" + UUID.randomUUID();
            CreateReactantDto requestDto = new CreateReactantDto(
                    "C1=CC=CC=C1O", chemicalType.getId(), shortName,
                    List.of("Alt1-" + UUID.randomUUID(), "Alt2-" + UUID.randomUUID()),
                    "Full Chemical Name",
                    "123-45-6",
                    "ABCDEFGHIJKLMN-OPQRSTUVWX-Y",
                    "Test comment for optional fields",
                    List.of("10.1000/scenario07-optional-ref"));

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
                    .andExpect(jsonPath("$.references[0]").value("10.1000/scenario07-optional-ref"))
                    .andExpect(jsonPath("$.archivedAt").doesNotExist());
        }
    }

    // ------------------------------------------------------------------------------------------
    // SCENARIO-08 - Options catalog creation: ADMIN succeeds, SUPERVISOR forbidden
    // ------------------------------------------------------------------------------------------
    @Nested
    class Scenario08OptionsCatalogRoleGate {

        // Representative of the 4 nearly-identical options endpoints (chemical type, solvent,
        // instrument, modality), which all share the same access rule. Only ADMIN can create a new
        // chemical type, so this succeeds (201), and a follow-up GET confirms the new option is
        // genuinely listed.
        @Test
        void adminCreatesChemicalType_succeeds() throws Exception {
            String adminId = "integration-scenario08-admin-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);

            String typeName = "Scenario08ChemType-" + UUID.randomUUID();

            mockMvc.perform(post("/api/chemical_type")
                            .with(asRealJwtSubject(adminId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + typeName + "\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItem(typeName)));

            // Independent follow-up read: a fresh GET, not the create response, confirms real
            // persistence.
            mockMvc.perform(get("/api/chemical_type")
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", hasItem(typeName)));
        }

        // A SUPERVISOR calling the same endpoint should be rejected with 403: unlike reactant
        // creation, the options catalog is ADMIN-only. A follow-up GET confirms nothing was
        // persisted.
        @Test
        void supervisorCreatesChemicalType_isForbidden() throws Exception {
            String supervisorId = "integration-scenario08-supervisor-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, supervisorId, Role.SUPERVISOR);

            String typeName = "Scenario08ForbiddenChemType-" + UUID.randomUUID();

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
    // SCENARIO-09 - Reactant archive: ADMIN succeeds, SUPERVISOR forbidden
    // ------------------------------------------------------------------------------------------
    @Nested
    class Scenario09ReactantArchiveRoleGate {

        // Archiving a reactant is ADMIN-only and returns 204. An ADMIN archives a reactant with no
        // experiments attached, and a follow-up GET confirms archivedAt is now set.
        @Test
        void adminArchivesReactant_succeedsAndArchivedAtSet() throws Exception {
            String adminId = "integration-scenario09-admin-" + UUID.randomUUID();
            User admin = IntegrationTestHelpers.persistUser(userRepository, adminId, Role.ADMIN);

            Reactant reactant = IntegrationTestHelpers.persistReactant(
                    reactantRepository, admin, "Scenario09AdminReactant-" + UUID.randomUUID());

            mockMvc.perform(delete("/api/reactant/{uuid}", reactant.getId())
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isNoContent());

            // Independent follow-up read: a fresh GET shows archivedAt now set.
            mockMvc.perform(get("/api/reactant/{uuid}", reactant.getId())
                            .with(asRealJwtSubject(adminId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.archivedAt").exists());
        }

        // A SUPERVISOR calling the same endpoint should be rejected with 403: unlike reactant
        // creation, archiving is treated as ADMIN-only. A follow-up GET confirms the reactant
        // remains unarchived.
        @Test
        void supervisorArchivesReactant_isForbiddenAndRemainsUnarchived() throws Exception {
            String supervisorId = "integration-scenario09-supervisor-" + UUID.randomUUID();
            User supervisor = IntegrationTestHelpers.persistUser(userRepository, supervisorId, Role.SUPERVISOR);

            Reactant reactant = IntegrationTestHelpers.persistReactant(
                    reactantRepository, supervisor, "Scenario09SupervisorReactant-" + UUID.randomUUID());

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
    // SCENARIO-10 - Metadata + rate-constant-range filtering, full real round trip
    // ------------------------------------------------------------------------------------------
    @Nested
    class Scenario10MetadataAndRateConstantFiltering {

        // Three experiments are seeded for the same scientist: one matching both a temperatureMin
        // filter and a nested measurementSearch.rateConstantMin filter, one failing only the
        // rate-constant filter, and one failing only the temperature filter, since both filters
        // must hold at once for a match. A single GET /api/experiment request with both query
        // parameters, scoped by scientist to this test's own unique creator so other tests' data
        // sharing the same H2 instance can't leak in, should return only the one experiment that
        // satisfies both filters.
        @Test
        void combinedTemperatureAndRateConstantFilter_returnsOnlyMatchingExperiment() throws Exception {
            String creatorId = "integration-scenario10-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Scenario10ReactantA-" + UUID.randomUUID());
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Scenario10ReactantB-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Scenario10Solvent-" + UUID.randomUUID());

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
    // SCENARIO-11 - Raw file retrieval round trip
    // ------------------------------------------------------------------------------------------
    @Nested
    class Scenario11RawFileRetrieval {

        private static final byte[] CANNED_BYTES = "time,signal\n0,1.0\n".getBytes(StandardCharsets.UTF_8);

        // A creator uploads a file to their own experiment, the mocked S3 client is stubbed to
        // return canned bytes on download, and the creator downloads the file back. The response
        // body should match those canned bytes exactly, with a Content-Disposition header marking
        // it as an attachment.
        @Test
        void creatorDownloadsFile_bytesMatchCannedS3Response() throws Exception {
            String creatorId = "integration-scenario11-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Scenario11ReactantA-" + UUID.randomUUID());
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Scenario11ReactantB-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Scenario11Solvent-" + UUID.randomUUID());

            UUID experimentId = createExperiment(creatorId, buildExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), 22.0, 4.0));

            MockMultipartFile csvFile = new MockMultipartFile(
                    "file", "scenario11-measurement.csv", "text/csv",
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

        // Any approved role can view an active experiment's files, even a plain USER who is
        // neither the creator nor the scientist. Only an archived experiment restricts viewing to
        // the creator, the scientist, or a privileged role. So to actually trigger a 403, the
        // experiment first needs to be archived (creators can always archive their own experiment,
        // regardless of its state), and only then does an unrelated USER get rejected when trying
        // to download a file from it.
        @Test
        void nonPrivilegedNonCreatorUser_forbiddenOnArchivedExperimentDownload() throws Exception {
            String creatorId = "integration-scenario11b-creator-" + UUID.randomUUID();
            User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

            String otherUserId = "integration-scenario11b-other-" + UUID.randomUUID();
            IntegrationTestHelpers.persistUser(userRepository, otherUserId, Role.USER);

            Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Scenario11bReactantA-" + UUID.randomUUID());
            Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                    reactantRepository, reactantStructureRepository, creator, "Scenario11bReactantB-" + UUID.randomUUID());
            Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "Scenario11bSolvent-" + UUID.randomUUID());

            UUID experimentId = createExperiment(creatorId, buildExperimentDto(
                    reactantA.getId(), reactantB.getId(), creatorId, solvent.getId(), 22.0, 4.0));

            MockMultipartFile csvFile = new MockMultipartFile(
                    "file", "scenario11b-measurement.csv", "text/csv",
                    "time,signal\n0,9.99\n".getBytes(StandardCharsets.UTF_8));

            String uploadResponseBody = mockMvc.perform(multipart("/api/file/{experimentId}", experimentId)
                            .file(csvFile)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID fileId = objectMapper.readValue(uploadResponseBody, ExperimentAttachmentDetailsDto.class).uuid();

            // The creator archives their own experiment: a real, allowed DELETE.
            mockMvc.perform(delete("/api/experiment/{uuid}", experimentId)
                            .with(asRealJwtSubject(creatorId)))
                    .andExpect(status().isNoContent());

            // otherUser is neither the creator, the scientist, nor privileged, and the experiment
            // is now archived, so ExperimentAuthorization.canView genuinely denies them.
            mockMvc.perform(get("/api/file/{fileId}", fileId)
                            .with(asRealJwtSubject(otherUserId)))
                    .andExpect(status().isForbidden());
        }
    }
}
