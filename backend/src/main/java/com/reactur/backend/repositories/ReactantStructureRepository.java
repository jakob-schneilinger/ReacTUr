package com.reactur.backend.repositories;

import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.enums.StructureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReactantStructureRepository extends JpaRepository<ReactantStructure, UUID> {

    List<ReactantStructure> findByReactant_Id(UUID id);

    /**
     * Batch variant of {@link #findByReactant_Id(UUID)}: fetches all structures for a collection
     * of reactant IDs in a single query, so callers that need structures for many reactants at
     * once (e.g. {@code ReactantServiceImpl}'s list endpoints) can avoid issuing one query per
     * reactant.
     *
     * @param reactantIds the reactant IDs to fetch structures for
     */
    List<ReactantStructure> findByReactant_IdIn(Collection<UUID> reactantIds);

    List<ReactantStructure> findByFlag(StructureFlag flag);
}
