package com.reactur.backend.repositories;

import com.reactur.backend.entities.Reactant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReactantRepository extends JpaRepository<Reactant, UUID> {

    List<Reactant> findAllByArchivedAtIsNull();

    List<Reactant> findAllByArchivedAtIsNotNull();

    @Query("""
        SELECT COUNT(r) > 0 FROM Reactant r 
        LEFT JOIN r.additionalShortNames asn 
        WHERE r.shortName IN :names OR asn IN :names
    """)
    boolean hasNameConflict(@Param("names") Collection<String> names);

    @Query("""
        SELECT COUNT(r) > 0 FROM Reactant r 
        LEFT JOIN r.additionalShortNames asn 
        WHERE (r.shortName IN :names OR asn IN :names) 
        AND r.id != :excludeId
    """)
    boolean hasNameConflictExcludingId(@Param("names") Collection<String> names, @Param("excludeId") UUID excludeId);

}
