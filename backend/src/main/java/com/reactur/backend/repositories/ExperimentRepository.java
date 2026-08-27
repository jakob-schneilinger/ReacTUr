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
   * Same as findById, but takes a DB-level pessimistic write lock on the row (a SQL SELECT FOR UPDATE) held for
   * the rest of the enclosing transaction. Used by ExperimentFolderServiceImpl.addExperimentToFolder to serialize
   * its one-folder-per-user check against other concurrent adds of the same experiment - see the Race condition
   * inventory item in Software_todo.md. Only safe inside a transaction whose critical section does no slow
   * external I/O while the lock is held.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM Experiment e WHERE e.id = :id")
  Optional<Experiment> findByIdForUpdate(@Param("id") java.util.UUID id);

  // Searches for all the experiments of user "userId" where the user is either the scientist associated with the
  // experiment, or the user that added the experiment to the DB
  @Query("SELECT e FROM Experiment e WHERE e.archivedAt IS NOT NULL AND (e.createdBy.id = :userId OR e.metadata.scientist.id = :userId)")
  List<Experiment> findArchivedExperimentsForUser(@Param("userId") String userId);
}
