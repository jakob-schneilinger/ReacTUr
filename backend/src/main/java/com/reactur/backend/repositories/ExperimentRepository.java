package com.reactur.backend.repositories;

import com.reactur.backend.entities.experiment.Experiment;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@JaversSpringDataAuditable
public interface ExperimentRepository extends JpaRepository<Experiment, UUID>, JpaSpecificationExecutor<Experiment> {

  List<Experiment> findByArchivedAtIsNotNull();

  // Searches for all the experiments of user "userId" where the user is either the scientist associated with the
  // experiment, or the user that added the experiment to the DB
  @Query("SELECT e FROM Experiment e WHERE e.archivedAt IS NOT NULL AND (e.createdBy.id = :userId OR e.metadata.scientist.id = :userId)")
  List<Experiment> findArchivedExperimentsForUser(@Param("userId") String userId);
}
