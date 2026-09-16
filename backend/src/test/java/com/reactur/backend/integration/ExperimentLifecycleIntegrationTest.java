package com.reactur.backend.integration;

import com.reactur.backend.client.StructureApiClient;
import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementDataDto;
import com.reactur.backend.dtos.experiments.SaveExperimentDto;
import com.reactur.backend.dtos.experiments.SaveMetadataDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.enums.Role;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier-1 integration tests: real Spring context, real H2 database, real security filter chain,
 * real {@code CustomJwtAuthenticationConverter}, real services/repositories.
 * <p>
 * <b>Important deviation from the originally sketched approach, and why:</b> the obvious way to
 * simulate a JWT in a Spring MVC test is {@code SecurityMockMvcRequestPostProcessors.jwt()}. That
 * post-processor, however - even when {@code .authorities(...)} is never called on it - injects a
 * ready-made {@code JwtAuthenticationToken} directly into the {@code SecurityContext} itself
 * (verified by decompiling {@code spring-security-test-7.0.5.jar}), using its own internal default
 * {@code JwtGrantedAuthoritiesConverter} (a scope-claim-based converter). It never invokes {@code
 * JwtDecoder}, and - critically for this test tier - it never invokes the app's real, wired-up
 * {@code customJwtAuthenticationConverter} bean either. Every request built that way would hit
 * {@code SecurityConfig}'s {@code anyRequest().hasAnyRole(...)} gate with zero role authorities
 * and always return 403, regardless of the seeded user's DB role - which was confirmed empirically
 * before this file was finalized.
 * <p>
 * To genuinely exercise the real {@code CustomJwtAuthenticationConverter} (and therefore the real
 * DB role lookup this tier exists to test), every request here instead sends a real {@code
 * Authorization: Bearer <token>} header and lets the real {@code BearerTokenAuthenticationFilter}
 * run. {@link JwtDecoder} is stubbed (it is one of the three explicitly-allowed mocks) to turn
 * that opaque token string into a {@link Jwt} carrying only a {@code sub} claim; from there, the
 * real {@code JwtAuthenticationProvider} hands that {@code Jwt} to the real, Spring-wired {@code
 * CustomJwtAuthenticationConverter}, which performs the real {@code UserRepository.findById}
 * lookup against the real H2 database and derives the {@code ROLE_*} authority accordingly - the
 * exact gap this integration tier exists to close.
 * <p>
 * Only three beans are mocked, because they represent genuinely external systems that must never
 * be called during a test run:
 * <ul>
 *     <li>{@link JwtDecoder} - no real Keycloak exists in this tier; stubbed per-subject below to
 *     return a bare {@code Jwt} (subject claim only), so all downstream role-derivation is still
 *     done for real.</li>
 *     <li>{@link StructureApiClient} - the external chemistry structure-generation API.</li>
 *     <li>{@link S3Client} - the SeaweedFS/S3 file storage backend.</li>
 * </ul>
 * Everything else - {@code UserRepository}, {@code ExperimentService}, {@code
 * ExperimentAuthorization}, mappers, validators, the security filter chain, {@code
 * CustomJwtAuthenticationConverter} itself - is real.
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  // SeaweedFsConfig#endpoint/accessKey/secretKey and StructureApiClientImpl's constructor
  // arg are required @Value properties with no defaults; application-test.yml intentionally doesn't define them.
  // Even though S3Client and StructureApiClient are @MockitoBean-overridden below, the
  // configuration/implementation classes that host them (SeaweedFsConfig,
  // StructureApiClientImpl) are still fully instantiated with real property injection
  // before any bean override is applied, so the full context still needs these properties
  // resolvable. Supplied here as harmless dummy values, scoped only to this test class,
  // rather than editing application-test.yml.
  properties = {
    "seaweedfs.s3.endpoint=http://localhost:8333",
    "seaweedfs.s3.access-key=integration-test-access-key",
    "seaweedfs.s3.secret-key=integration-test-secret-key",
    "external-api.reactur.base-url=http://localhost:8000"
  })
@AutoConfigureMockMvc
@ActiveProfiles("test")
// @Transactional wraps each test method in a real transaction that's rolled back at the end.
// Without this, every real MockMvc-driven write here would actually commit to the shared, named
// H2 instance (jdbc:h2:mem:kineticsdb) and could leak into other test
// classes depending on execution order/context lifecycle.
@Transactional
class ExperimentLifecycleIntegrationTest {

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

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private StructureApiClient structureApiClient;

    @MockitoBean
    private S3Client s3Client;

    // Stubs the mocked JwtDecoder so the bearer token keycloakId decodes to a Jwt with just that
    // subject claim, then returns a RequestPostProcessor that attaches it as an Authorization
    // header. This lets the request go through the real authentication filter and converter
    // instead of the jwt() post-processor.
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

    // Scenario A: full happy-path round trip. A user acts as both creator and designated
    // scientist. Two reactants (each with an ACTIVE structure), a solvent, an instrument, and a
    // modality are persisted first. The user then POSTs a valid experiment, which goes through the
    // whole real stack: authentication, the security filter chain, validation against the
    // database, and finally saving.
    //
    // A second, independent GET request for the new experiment's UUID then confirms the data was
    // actually persisted, not just echoed back in the response.
    @Test
    void scenarioA_createThenGet_realRoundTripPersistsAndReturnsMatchingData() throws Exception {
        String creatorId = "integration-creator-" + UUID.randomUUID();
        User creator = IntegrationTestHelpers.persistUser(userRepository, creatorId, Role.USER);

        Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                reactantRepository, reactantStructureRepository, creator, "IntReactantA-" + UUID.randomUUID());
        Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                reactantRepository, reactantStructureRepository, creator, "IntReactantB-" + UUID.randomUUID());
        Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "IntSolvent-" + UUID.randomUUID());
        Instrument instrument = IntegrationTestHelpers.persistInstrument(instrumentRepository, "IntInstrument-" + UUID.randomUUID());
        Modality modality = IntegrationTestHelpers.persistModality(modalityRepository, "IntModality-" + UUID.randomUUID());

        SaveMetadataDto metadata = new SaveMetadataDto(
                LocalDate.now().minusDays(1),
                creatorId, // Scientist == creator, both are the same real persisted User.
                25.0,
                solvent.getId(),
                1.5,
                2.0,
                instrument.getId(),
                modality.getId(),
                450.0,
                "Tier-1 integration test happy path"
        );
        MeasurementDataDto measurement = new MeasurementDataDto(1.2, 5.5, 0.1, false);
        SaveExperimentDto requestDto = new SaveExperimentDto(
                reactantA.getId(), reactantB.getId(), metadata, measurement, List.of("10.1000/integration-test")
        );

        String responseBody = mockMvc.perform(post("/api/experiment")
                        .with(asRealJwtSubject(creatorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andReturn().getResponse().getContentAsString();

        GetExperimentDto created = objectMapper.readValue(responseBody, GetExperimentDto.class);
        assertNotNull(created.uuid(), "Created experiment must have a non-null uuid");

        // Second, independent real request: proves the data was actually persisted, not merely
        // echoed back by a mocked service.
        mockMvc.perform(get("/api/experiment/{uuid}", created.uuid())
                        .with(asRealJwtSubject(creatorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(created.uuid().toString()))
                .andExpect(jsonPath("$.dois[0]").value("10.1000/integration-test"))
                .andExpect(jsonPath("$.reactantA.uuid").value(reactantA.getId().toString()))
                .andExpect(jsonPath("$.reactantB.uuid").value(reactantB.getId().toString()))
                .andExpect(jsonPath("$.metadata.temperature").value(25.0))
                .andExpect(jsonPath("$.metadata.solvent.uuid").value(solvent.getId().toString()))
                .andExpect(jsonPath("$.metadata.instrument.uuid").value(instrument.getId().toString()))
                .andExpect(jsonPath("$.metadata.modality.uuid").value(modality.getId().toString()))
                .andExpect(jsonPath("$.measurementData.rateConstant").value(5.5))
                .andExpect(jsonPath("$.archivedAt").doesNotExist());
    }

    // Scenario B: cross-user authorization. UserA creates an experiment (both creator and
    // scientist). UserB, a different user with no relation to it, then tries to archive it. Only
    // the creator, the designated scientist, or a privileged role may modify an experiment, so this
    // should be rejected with 403. A follow-up GET as UserA confirms archivedAt is still null,
    // proving the request was actually blocked rather than just returning an error code without
    // changing anything.
    @Test
    void scenarioB_deleteByNonCreatorNonScientist_isForbiddenAndDoesNotArchive() throws Exception {
        String userAId = "integration-usera-" + UUID.randomUUID();
        User userA = IntegrationTestHelpers.persistUser(userRepository, userAId, Role.USER);

        String userBId = "integration-userb-" + UUID.randomUUID();
        IntegrationTestHelpers.persistUser(userRepository, userBId, Role.USER);

        Reactant reactantA = IntegrationTestHelpers.persistReactantWithActiveStructure(
                reactantRepository, reactantStructureRepository, userA, "IntReactantA-" + UUID.randomUUID());
        Reactant reactantB = IntegrationTestHelpers.persistReactantWithActiveStructure(
                reactantRepository, reactantStructureRepository, userA, "IntReactantB-" + UUID.randomUUID());
        Solvent solvent = IntegrationTestHelpers.persistSolvent(solventRepository, "IntSolvent-" + UUID.randomUUID());

        SaveMetadataDto metadata = new SaveMetadataDto(
                LocalDate.now().minusDays(1),
                userAId, // UserA is both creator and scientist.
                20.0,
                solvent.getId(),
                1.0,
                1.0,
                null,
                null,
                null,
                "Tier-1 integration test authorization scenario"
        );
        MeasurementDataDto measurement = new MeasurementDataDto(null, 3.3, 0.2, true);
        SaveExperimentDto requestDto = new SaveExperimentDto(
                reactantA.getId(), reactantB.getId(), metadata, measurement, List.of()
        );

        String responseBody = mockMvc.perform(post("/api/experiment")
                        .with(asRealJwtSubject(userAId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID experimentId = objectMapper.readValue(responseBody, GetExperimentDto.class).uuid();

        // UserB is real and persisted, but neither creator nor scientist, so this must be forbidden.
        mockMvc.perform(delete("/api/experiment/{uuid}", experimentId)
                        .with(asRealJwtSubject(userBId)))
                .andExpect(status().isForbidden());

        // Confirm the real DB row was never mutated: archivedAt must still be null.
        mockMvc.perform(get("/api/experiment/{uuid}", experimentId)
                        .with(asRealJwtSubject(userAId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").doesNotExist());
    }

    // Scenario C: a user with Role.UNAUTHORIZED calls GET /api/experiment and is rejected with
    // 403. The role that got them rejected came from an actual database lookup on the JWT subject,
    // not from anything hardcoded in the test. As a positive counterpart, a second user with
    // Role.USER makes the same call and gets 200, showing it's the database state, not the test
    // setup, that decides the outcome.
    @Test
    void scenarioC_realConverterDbRoleLookup_determinesAuthorizationOutcome() throws Exception {
        String unauthorizedUserId = "integration-unauthorized-" + UUID.randomUUID();
        IntegrationTestHelpers.persistUser(userRepository, unauthorizedUserId, Role.UNAUTHORIZED);

        String approvedUserId = "integration-approved-" + UUID.randomUUID();
        IntegrationTestHelpers.persistUser(userRepository, approvedUserId, Role.USER);

        mockMvc.perform(get("/api/experiment")
                        .with(asRealJwtSubject(unauthorizedUserId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/experiment")
                        .with(asRealJwtSubject(approvedUserId)))
                .andExpect(status().isOk());
    }
}
