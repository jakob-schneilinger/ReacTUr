package com.reactur.backend.services;

import com.reactur.backend.dtos.experiments.GetExperimentDto;
import com.reactur.backend.dtos.experiments.MeasurementSearchDto;
import com.reactur.backend.dtos.experiments.SearchParametersDto;
import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.entities.experiment.ExperimentResult;
import com.reactur.backend.entities.experiment.Metadata;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.enums.Role;
import com.reactur.backend.mappers.ExperimentMapper;
import com.reactur.backend.repositories.ExperimentRepository;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;
import com.reactur.backend.services.authorization.ExperimentAuthorization;
import com.reactur.backend.services.implementations.ExperimentServiceImpl;
import com.reactur.backend.validators.ExperimentValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link ExperimentServiceImpl#buildSearchSpecification} against a real database:
 * reactant/metadata/measurement filters actually narrow the result set correctly, including the
 * reactant commutativity rule and combining multiple filters together.
 */
// Same rationale as ReactantRepositoryTest/ExperimentRepositoryTest: @DataJpaTest +
// @AutoConfigureTestDatabase(Replace.NONE) + @ActiveProfiles("test") together ensure the real
// H2 "test" profile database (src/test/resources/application-test.yml) is used instead of Spring
// Boot's silently auto-configured replacement database.
//
// @Import(ExperimentServiceImpl.class) registers the REAL service in this @DataJpaTest context so
// it is wired with the REAL, H2-backed ExperimentRepository. This lets buildSearchSpecification's
// Specification lambda actually run against real persisted data, rather than only proving (as
// ExperimentServiceTest's mocked-repository test does) that the repository method gets invoked.
//
// experimentMapper is stubbed (not wired real) purely for assertion convenience: this test only
// cares about *which* underlying Experiment entities matched the filter, so the stub just carries
// the source experiment's id through to the returned DTO's id field.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ExperimentServiceImpl.class)
class ExperimentSearchSpecificationTest {

    @Autowired
    private ExperimentServiceImpl experimentService;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ReactantRepository reactantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SolventRepository solventRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private ModalityRepository modalityRepository;

    @MockitoBean
    private ExperimentMapper experimentMapper;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private ExperimentAuthorization experimentAuthorization;

    @MockitoBean
    private ExperimentValidator experimentValidator;

    private final Pageable defaultPageable = PageRequest.of(0, 20);

    private void setUpMapperAndFileServiceStubs() {
        when(fileService.getAttachmentsOfExperiment(any())).thenReturn(List.of());
        when(experimentMapper.experimentToGetExperimentDto(any(), any()))
                .thenAnswer(invocation -> {
                    Experiment exp = invocation.getArgument(0);
                    return new GetExperimentDto(
                            exp.getId(), null, null, null, null, null, null, null, null
                    );
                });
    }

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

    private Solvent persistSolvent(String name) {
        return solventRepository.saveAndFlush(Solvent.builder().name(name).build());
    }

    private Instrument persistInstrument(String name) {
        return instrumentRepository.saveAndFlush(Instrument.builder().name(name).build());
    }

    private Modality persistModality(String name) {
        return modalityRepository.saveAndFlush(Modality.builder().name(name).build());
    }

    // Flexible builder-style helper covering all fixture attributes exercised by this test class.
    private Experiment persistExperiment(
            User createdBy,
            Reactant reactantA,
            Reactant reactantB,
            User scientist,
            Double temperature,
            String comment,
            Solvent solvent,
            Instrument instrument,
            Modality modality,
            Double rateConstant,
            Double pfoRateConstant,
            Instant archivedAt
    ) {
        Metadata metadata = Metadata.builder()
                .date(LocalDate.now())
                .scientist(scientist)
                .temperature(temperature)
                .comment(comment)
                .solvent(solvent)
                .instrument(instrument)
                .modality(modality)
                .build();

        ExperimentResult result = ExperimentResult.builder()
                .rateConstant(rateConstant != null ? rateConstant : 1.0) // rate_constant column is NOT NULL
                .pfoRateConstant(pfoRateConstant)
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

    // Deliberately a distinct name (not an overload of persistExperiment): several existing
    // persistExperiment(...) call sites above pass null for every trailing argument, which would
    // make a same-arity overload ambiguous to the compiler. Covers fields (date, concentrationA/B,
    // wavelength) not exercised by any existing test filter prior to this addition.
    private Experiment persistExperimentWithExtendedMetadata(
            User createdBy,
            Reactant reactantA,
            Reactant reactantB,
            User scientist,
            Solvent solvent,
            Instrument instrument,
            Modality modality,
            Double pfoRateConstant,
            LocalDate date,
            Double concentrationA,
            Double concentrationB,
            Double wavelength
    ) {
        Metadata metadata = Metadata.builder()
                .date(date)
                .scientist(scientist)
                .solvent(solvent)
                .instrument(instrument)
                .modality(modality)
                .concentrationA(concentrationA)
                .concentrationB(concentrationB)
                .wavelength(wavelength)
                .build();

        ExperimentResult result = ExperimentResult.builder()
                .rateConstant(1.0) // rate_constant column is NOT NULL
                .pfoRateConstant(pfoRateConstant)
                .errorValue(0.1)
                .isErrorPercentage(false)
                .build();

        Experiment experiment = Experiment.builder()
                .reactantA(reactantA)
                .reactantB(reactantB)
                .metadata(metadata)
                .result(result)
                .createdBy(createdBy)
                .build();
        return experimentRepository.saveAndFlush(experiment);
    }

    private SearchParametersDto emptyParams() {
        return new SearchParametersDto(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null
        );
    }

    private SearchParametersDto withReactants(List<UUID> reactantA, List<UUID> reactantB) {
        return new SearchParametersDto(
                reactantA, reactantB, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null
        );
    }

    private SearchParametersDto withTemperatureRange(Double min, Double max) {
        return new SearchParametersDto(
                null, null, null, null, null, min, max, null,
                null, null, null, null, null, null, null, null, null, null
        );
    }

    // Field order: reactantA, reactantB, dateMin, dateMax, scientist, temperatureMin, temperatureMax,
    // solvent, concentrationA_Min, concentrationA_Max, concentrationB_Min, concentrationB_Max,
    // instrument, modality, wavelengthMin, wavelengthMax, comment, measurementSearch
    private SearchParametersDto withComment(String comment) {
        return new SearchParametersDto(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, comment, null
        );
    }

    private SearchParametersDto withScientists(List<String> scientistIds) {
        return new SearchParametersDto(
                null, null, null, null, scientistIds, null, null, null,
                null, null, null, null, null, null, null, null, null, null
        );
    }

    private SearchParametersDto withSolvent(UUID solventId) {
        return new SearchParametersDto(
                null, null, null, null, null, null, null, solventId,
                null, null, null, null, null, null, null, null, null, null
        );
    }

    private SearchParametersDto withMeasurement(MeasurementSearchDto measurementSearch) {
        return new SearchParametersDto(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, measurementSearch
        );
    }

    private SearchParametersDto withTemperatureRangeAndScientists(Double min, Double max, List<String> scientistIds) {
        return new SearchParametersDto(
                null, null, null, null, scientistIds, min, max, null,
                null, null, null, null, null, null, null, null, null, null
        );
    }

    private SearchParametersDto withDateRange(LocalDate min, LocalDate max) {
        return new SearchParametersDto(
                null, null, min, max, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null
        );
    }

    private SearchParametersDto withConcentrationARange(Double min, Double max) {
        return new SearchParametersDto(
                null, null, null, null, null, null, null, null,
                min, max, null, null, null, null, null, null, null, null
        );
    }

    private SearchParametersDto withConcentrationBRange(Double min, Double max) {
        return new SearchParametersDto(
                null, null, null, null, null, null, null, null,
                null, null, min, max, null, null, null, null, null, null
        );
    }

    private SearchParametersDto withInstrument(UUID instrumentId) {
        return new SearchParametersDto(
                null, null, null, null, null, null, null, null,
                null, null, null, null, instrumentId, null, null, null, null, null
        );
    }

    private SearchParametersDto withModality(UUID modalityId) {
        return new SearchParametersDto(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, modalityId, null, null, null, null
        );
    }

    private SearchParametersDto withWavelengthRange(Double min, Double max) {
        return new SearchParametersDto(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, min, max, null, null
        );
    }

    private List<UUID> resultIds(Page<GetExperimentDto> page) {
        return page.getContent().stream().map(GetExperimentDto::uuid).toList();
    }

    @Test
    void searchExperiments_shouldNeverReturnArchivedExperiments_regardlessOfOtherFilters() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        Experiment active = persistExperiment(user, a, b, user, 25.0, null, null, null, null, null, null, null);
        persistExperiment(user, a, b, user, 25.0, null, null, null, null, null, null, Instant.now());

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                emptyParams(), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactly(active.getId());
    }

    @Test
    void searchExperiments_withNullParams_returnsAllNonArchivedExperiments() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        Experiment first = persistExperiment(user, a, b, user, 25.0, null, null, null, null, null, null, null);
        Experiment second = persistExperiment(user, a, b, user, 30.0, null, null, null, null, null, null, null);
        persistExperiment(user, a, b, user, 20.0, null, null, null, null, null, null, Instant.now());

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                null, defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void searchExperiments_reactantFiltering_matchesAOnlyBOnlyAndCommutativeCross() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant r1 = persistReactant("R1-" + UUID.randomUUID(), user);
        Reactant r2 = persistReactant("R2-" + UUID.randomUUID(), user);
        Reactant r3 = persistReactant("R3-" + UUID.randomUUID(), user);
        Reactant r4 = persistReactant("R4-" + UUID.randomUUID(), user);

        // Direct experiment: reactantA=r1, reactantB=r2
        Experiment direct = persistExperiment(user, r1, r2, user, null, null, null, null, null, null, null, null);
        // Swapped experiment: persisted reactantA=r2, reactantB=r1 (cross-match candidate)
        Experiment swapped = persistExperiment(user, r2, r1, user, null, null, null, null, null, null, null, null);
        // Unrelated experiment using entirely different reactants
        persistExperiment(user, r3, r4, user, null, null, null, null, null, null, null, null);

        // A-only: search for r1 in either column
        Page<GetExperimentDto> aOnly = experimentService.searchExperiments(
                withReactants(List.of(r1.getId()), null), defaultPageable, user.getId(), Role.USER
        );
        assertThat(resultIds(aOnly)).containsExactlyInAnyOrder(direct.getId(), swapped.getId());

        // B-only: search for r2 in either column
        Page<GetExperimentDto> bOnly = experimentService.searchExperiments(
                withReactants(null, List.of(r2.getId())), defaultPageable, user.getId(), Role.USER
        );
        assertThat(resultIds(bOnly)).containsExactlyInAnyOrder(direct.getId(), swapped.getId());

        // Both A and B provided: direct match (A=r1,B=r2) AND cross/swapped match (A=r2,B=r1) both found
        Page<GetExperimentDto> both = experimentService.searchExperiments(
                withReactants(List.of(r1.getId()), List.of(r2.getId())), defaultPageable, user.getId(), Role.USER
        );
        assertThat(resultIds(both)).containsExactlyInAnyOrder(direct.getId(), swapped.getId());
    }

    @Test
    void searchExperiments_temperatureRangeFilter_isBoundaryInclusiveAndExcludesOutOfRange() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        Experiment atMin = persistExperiment(user, a, b, user, 10.0, null, null, null, null, null, null, null);
        Experiment atMax = persistExperiment(user, a, b, user, 20.0, null, null, null, null, null, null, null);
        persistExperiment(user, a, b, user, 9.9, null, null, null, null, null, null, null);
        persistExperiment(user, a, b, user, 20.1, null, null, null, null, null, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withTemperatureRange(10.0, 20.0), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactlyInAnyOrder(atMin.getId(), atMax.getId());
    }

    @Test
    void searchExperiments_commentFilter_isCaseInsensitivePartialMatch() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        Experiment matching = persistExperiment(user, a, b, user, null, "Contains SPECIAL keyword here", null, null, null, null, null, null);
        persistExperiment(user, a, b, user, null, "unrelated text", null, null, null, null, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withComment("special"), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactly(matching.getId());
    }

    @Test
    void searchExperiments_scientistFilter_matchesInListOfScientistIds() {
        setUpMapperAndFileServiceStubs();
        User owner = persistUser("owner");
        User scientistOne = persistUser("scientist-one");
        User scientistTwo = persistUser("scientist-two");
        Reactant a = persistReactant("A" + UUID.randomUUID(), owner);
        Reactant b = persistReactant("B" + UUID.randomUUID(), owner);

        Experiment byOne = persistExperiment(owner, a, b, scientistOne, null, null, null, null, null, null, null, null);
        persistExperiment(owner, a, b, scientistTwo, null, null, null, null, null, null, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withScientists(List.of(scientistOne.getId())), defaultPageable, owner.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactly(byOne.getId());
    }

    @Test
    void searchExperiments_solventFilter_matchesExactly() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);
        Solvent water = persistSolvent("Water-" + UUID.randomUUID());
        Solvent ethanol = persistSolvent("Ethanol-" + UUID.randomUUID());

        Experiment withWater = persistExperiment(user, a, b, user, null, null, water, null, null, null, null, null);
        persistExperiment(user, a, b, user, null, null, ethanol, null, null, null, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withSolvent(water.getId()), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactly(withWater.getId());
    }

    @Test
    void searchExperiments_measurementSearchFilter_matchesRateConstantRangeViaResultJoin() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        Experiment inRange = persistExperiment(user, a, b, user, null, null, null, null, null, 5.0, null, null);
        persistExperiment(user, a, b, user, null, null, null, null, null, 100.0, null, null);

        MeasurementSearchDto measurementSearch = new MeasurementSearchDto(null, null, 1.0, 10.0);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withMeasurement(measurementSearch), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactly(inRange.getId());
    }

    @Test
    void searchExperiments_combinesTemperatureAndScientistFilters_withAndLogic() {
        setUpMapperAndFileServiceStubs();
        User owner = persistUser("owner");
        User scientistMatch = persistUser("scientist-match");
        User scientistOther = persistUser("scientist-other");
        Reactant a = persistReactant("A" + UUID.randomUUID(), owner);
        Reactant b = persistReactant("B" + UUID.randomUUID(), owner);

        // Matches both filters
        Experiment matchesBoth = persistExperiment(owner, a, b, scientistMatch, 25.0, null, null, null, null, null, null, null);
        // Matches temperature only (wrong scientist)
        persistExperiment(owner, a, b, scientistOther, 25.0, null, null, null, null, null, null, null);
        // Matches scientist only (wrong temperature)
        persistExperiment(owner, a, b, scientistMatch, 5.0, null, null, null, null, null, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withTemperatureRangeAndScientists(20.0, 30.0, List.of(scientistMatch.getId())),
                defaultPageable, owner.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactly(matchesBoth.getId());
    }

    @Test
    void searchExperiments_dateRangeFilter_isBoundaryInclusiveAndExcludesOutOfRange() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        LocalDate min = LocalDate.of(2024, 1, 10);
        LocalDate max = LocalDate.of(2024, 1, 20);

        Experiment atMin = persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, min, null, null, null);
        Experiment atMax = persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, max, null, null, null);
        persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, min.minusDays(1), null, null, null);
        persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, max.plusDays(1), null, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withDateRange(min, max), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactlyInAnyOrder(atMin.getId(), atMax.getId());
    }

    @Test
    void searchExperiments_concentrationARangeFilter_isBoundaryInclusiveAndExcludesOutOfRange() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        Experiment atMin = persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), 1.0, null, null);
        Experiment atMax = persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), 5.0, null, null);
        persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), 0.9, null, null);
        persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), 5.1, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withConcentrationARange(1.0, 5.0), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactlyInAnyOrder(atMin.getId(), atMax.getId());
    }

    @Test
    void searchExperiments_concentrationBRangeFilter_isBoundaryInclusiveAndExcludesOutOfRange() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        Experiment atMin = persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), null, 1.0, null);
        Experiment atMax = persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), null, 5.0, null);
        persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), null, 0.9, null);
        persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), null, 5.1, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withConcentrationBRange(1.0, 5.0), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactlyInAnyOrder(atMin.getId(), atMax.getId());
    }

    @Test
    void searchExperiments_wavelengthRangeFilter_isBoundaryInclusiveAndExcludesOutOfRange() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        Experiment atMin = persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), null, null, 400.0);
        Experiment atMax = persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), null, null, 700.0);
        persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), null, null, 399.9);
        persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, null, LocalDate.now(), null, null, 700.1);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withWavelengthRange(400.0, 700.0), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactlyInAnyOrder(atMin.getId(), atMax.getId());
    }

    @Test
    void searchExperiments_instrumentFilter_matchesExactly() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);
        Instrument nmr = persistInstrument("NMR-" + UUID.randomUUID());
        Instrument uv = persistInstrument("UV-" + UUID.randomUUID());

        Experiment withNmr = persistExperiment(user, a, b, user, null, null, null, nmr, null, null, null, null);
        persistExperiment(user, a, b, user, null, null, null, uv, null, null, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withInstrument(nmr.getId()), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactly(withNmr.getId());
    }

    @Test
    void searchExperiments_modalityFilter_matchesExactly() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);
        Modality absorption = persistModality("Absorption-" + UUID.randomUUID());
        Modality emission = persistModality("Emission-" + UUID.randomUUID());

        Experiment withAbsorption = persistExperiment(user, a, b, user, null, null, null, null, absorption, null, null, null);
        persistExperiment(user, a, b, user, null, null, null, null, emission, null, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withModality(absorption.getId()), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactly(withAbsorption.getId());
    }

    @Test
    void searchExperiments_measurementSearchFilter_matchesPfoRateConstantRangeViaResultJoin() {
        setUpMapperAndFileServiceStubs();
        User user = persistUser("owner");
        Reactant a = persistReactant("A" + UUID.randomUUID(), user);
        Reactant b = persistReactant("B" + UUID.randomUUID(), user);

        Experiment inRange = persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, 5.0, LocalDate.now(), null, null, null);
        persistExperimentWithExtendedMetadata(user, a, b, user, null, null, null, 100.0, LocalDate.now(), null, null, null);

        MeasurementSearchDto measurementSearch = new MeasurementSearchDto(1.0, 10.0, null, null);

        Page<GetExperimentDto> result = experimentService.searchExperiments(
                withMeasurement(measurementSearch), defaultPageable, user.getId(), Role.USER
        );

        assertThat(resultIds(result)).containsExactly(inRange.getId());
    }
}
