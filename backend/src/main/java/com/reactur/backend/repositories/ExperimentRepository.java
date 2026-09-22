package com.reactur.backend.repositories;

import com.reactur.backend.entities.experiment.Experiment;
import jakarta.persistence.LockModeType;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@JaversSpringDataAuditable
public interface ExperimentRepository extends JpaRepository<Experiment, UUID>, JpaSpecificationExecutor<Experiment> {

  List<Experiment> findByArchivedAtIsNotNull();

  /**
   * Same as {@code findById}, but takes a DB-level pessimistic write lock on the row (a SQL
   * {@code SELECT ... FOR UPDATE}) held for the rest of the enclosing transaction. Used to
   * serialize a per-experiment invariant check against other concurrent operations on the same
   * experiment. Only safe inside a transaction whose critical section does no slow external I/O
   * while the lock is held.
   *
   * @param id the ID of the experiment to lock
   * @return the locked experiment, if it exists
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM Experiment e WHERE e.id = :id")
  Optional<Experiment> findByIdForUpdate(@Param("id") java.util.UUID id);

  /**
   * Finds every archived experiment where {@code userId} is either the designated scientist or
   * the user who created the experiment record.
   *
   * @param userId the Keycloak ID to search by
   * @return the matching archived experiments
   */
  @Query("SELECT e FROM Experiment e WHERE e.archivedAt IS NOT NULL AND (e.createdBy.id = :userId OR e.metadata.scientist.id = :userId)")
  List<Experiment> findArchivedExperimentsForUser(@Param("userId") String userId);
}
