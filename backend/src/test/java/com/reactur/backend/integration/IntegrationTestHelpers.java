package com.reactur.backend.integration;

import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.entities.User;
import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.enums.Role;
import com.reactur.backend.enums.StructureFlag;
import com.reactur.backend.repositories.ReactantRepository;
import com.reactur.backend.repositories.ReactantStructureRepository;
import com.reactur.backend.repositories.UserRepository;
import com.reactur.backend.repositories.options.InstrumentRepository;
import com.reactur.backend.repositories.options.ModalityRepository;
import com.reactur.backend.repositories.options.SolventRepository;

/**
 * Small, deliberately dumb seeding helpers shared by the Tier-1 integration tests.
 * <p>
 * Every method here persists a real, minimal-but-valid entity via a real (non-mocked) repository,
 * so that the tests exercise the actual H2 database rather than any in-memory stand-in. These are
 * plain static helpers (not a base test class) - each integration test class wires its own
 * {@code @Autowired} repositories and passes them in explicitly.
 */
final class IntegrationTestHelpers {

    private IntegrationTestHelpers() {
    }

    /**
     * Persists a {@link User} whose {@code id} IS the Keycloak subject that will later be passed
     * to {@code jwt().jwt(builder -> builder.subject(...))} in a test - see
     * {@code CustomJwtAuthenticationConverter#convert}, which looks the JWT subject up directly as
     * {@code User.id}.
     */
    static User persistUser(UserRepository userRepository, String keycloakId, Role role) {
        User user = User.builder()
                .id(keycloakId)
                .email(keycloakId + "@integration-test.reactur")
                .firstName("Integration")
                .lastName("Tester")
                .role(role)
                .build();
        return userRepository.save(user);
    }

    /**
     * Persists a {@link Reactant} plus an ACTIVE {@link ReactantStructure} for it, since
     * {@code ExperimentValidator#validateReactantHasActiveStructure} requires at least one ACTIVE
     * structure before the reactant can be referenced by an experiment.
     */
    static Reactant persistReactantWithActiveStructure(
            ReactantRepository reactantRepository,
            ReactantStructureRepository structureRepository,
            User createdBy,
            String shortName
    ) {
        Reactant reactant = Reactant.builder()
                .shortName(shortName)
                .smiles("C1=CC=CC=C1")
                .createdBy(createdBy)
                .build();
        reactant = reactantRepository.save(reactant);

        ReactantStructure structure = ReactantStructure.builder()
                .reactant(reactant)
                .fileName(shortName + ".mol")
                .structure("mock-structure-payload")
                .flag(StructureFlag.ACTIVE)
                .build();
        structureRepository.save(structure);

        return reactant;
    }

    /**
     * Persists a bare {@link Reactant} with no associated {@link ReactantStructure} at all -
     * needed for scenarios exercising the structure-generation lifecycle from a genuinely clean
     * starting point (e.g. INT-05's "real Reactant with no existing structures").
     */
    static Reactant persistReactant(ReactantRepository reactantRepository, User createdBy, String shortName) {
        Reactant reactant = Reactant.builder()
                .shortName(shortName)
                .smiles("C1=CC=CC=C1")
                .createdBy(createdBy)
                .build();
        return reactantRepository.save(reactant);
    }

    /**
     * Persists a {@link Reactant} plus a single {@link ReactantStructure} carrying the given
     * {@link StructureFlag} - mirrors {@link #persistReactantWithActiveStructure} but for
     * non-ACTIVE flags (e.g. PENDING/READY), needed for scenarios asserting that a reactant
     * without an ACTIVE structure cannot yet be used in an experiment (INT-03).
     */
    static Reactant persistReactantWithStructure(
            ReactantRepository reactantRepository,
            ReactantStructureRepository structureRepository,
            User createdBy,
            String shortName,
            StructureFlag flag
    ) {
        Reactant reactant = persistReactant(reactantRepository, createdBy, shortName);

        ReactantStructure structure = ReactantStructure.builder()
                .reactant(reactant)
                .fileName(shortName + ".mol")
                .structure("mock-structure-payload")
                .flag(flag)
                .build();
        structureRepository.save(structure);

        return reactant;
    }

    static Solvent persistSolvent(SolventRepository solventRepository, String name) {
        return solventRepository.save(Solvent.builder().name(name).build());
    }

    static Instrument persistInstrument(InstrumentRepository instrumentRepository, String name) {
        return instrumentRepository.save(Instrument.builder().name(name).build());
    }

    static Modality persistModality(ModalityRepository modalityRepository, String name) {
        return modalityRepository.save(Modality.builder().name(name).build());
    }
}
