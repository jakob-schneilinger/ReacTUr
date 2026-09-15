package com.reactur.backend.repositories;

import com.reactur.backend.entities.Reactant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReactantRepository extends JpaRepository<Reactant, UUID> {

    List<Reactant> findAllByArchivedAtIsNull();

    List<Reactant> findAllByArchivedAtIsNotNull();

    /**
     * Same as findById, but takes a DB-level pessimistic write lock on the row (a SQL SELECT FOR UPDATE) held for
     * the rest of the enclosing transaction. Used wherever a multi-row invariant tied to this reactant (e.g. exactly
     * one ACTIVE structure, or has an ACTIVE structure at all) needs to be read-then-written atomically against
     * other transactions touching the same reactant's structures - see StructureServiceImpl uploadStructure and
     * selectStructure, and ExperimentValidator validateReactantHasActiveStructure, plus the Race condition
     * inventory item in Software_todo.md. Only safe inside a transaction whose critical section does no slow
     * external I/O (network calls) while the lock is held - do not use this around S3/HTTP calls.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reactant r WHERE r.id = :id")
    Optional<Reactant> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Checks whether any existing reactant already uses one of the given names, either as its
     * primary {@code shortName} or as one of its {@code additionalShortNames}.
     * <p>
     * <b>Contract:</b> the comparison is case-insensitive, but JPQL's {@code LOWER(...)} only
     * lower-cases a single scalar expression, not each element of a bound {@code IN} collection.
     * Every element of {@code names} MUST therefore already be lowercased by the caller before
     * being passed in (see {@code ReactantValidator.collectNames}); this method does not do it for
     * you.
     * <p>
     * TODO: check-then-save race - not atomic without a DB-level unique constraint (needs a
     * migration-tooling decision first). To review in the JavaDoc pass.
     *
     * @param names candidate short names to check for conflicts; MUST already be lowercased
     */
    @Query("""
        SELECT COUNT(r) > 0 FROM Reactant r
        LEFT JOIN r.additionalShortNames asn
        WHERE LOWER(r.shortName) IN :names OR LOWER(asn) IN :names
    """)
    boolean hasNameConflict(@Param("names") Collection<String> names);

    /**
     * Same as {@link #hasNameConflict(Collection)}, excluding a given reactant id (used when
     * updating an existing reactant so that it doesn't conflict with its own current names).
     * <p>
     * <b>Contract:</b> as with {@link #hasNameConflict(Collection)}, {@code names} MUST already be
     * lowercased by the caller — {@code LOWER(...)} here only applies to the scalar entity fields,
     * not to the elements of the bound {@code names} collection.
     *
     * @param names candidate short names to check for conflicts; MUST already be lowercased
     * @param excludeId id of the reactant to exclude from the conflict check
     */
    @Query("""
        SELECT COUNT(r) > 0 FROM Reactant r
        LEFT JOIN r.additionalShortNames asn
        WHERE (LOWER(r.shortName) IN :names OR LOWER(asn) IN :names)
        AND r.id != :excludeId
    """)
    boolean hasNameConflictExcludingId(@Param("names") Collection<String> names, @Param("excludeId") UUID excludeId);

    boolean existsByCasNumber(String casNumber);
    boolean existsByInChiKey(String inChiKey);

    boolean existsByCasNumberAndIdNot(String casNumber, UUID id);
    boolean existsByInChiKeyAndIdNot(String inChiKey, UUID id);
}
