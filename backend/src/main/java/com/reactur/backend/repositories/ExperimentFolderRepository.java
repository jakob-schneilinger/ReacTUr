package com.reactur.backend.repositories;

import com.reactur.backend.entities.experiment.ExperimentFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExperimentFolderRepository extends JpaRepository<ExperimentFolder, UUID> {

  /**
   * Retrieves all folders belonging to a specific user.
   * Users cannot see folders created by others.
   *
   * @param ownerId The internal Keycloak ID of the requesting user.
   * @return A list of folders owned by the specified user.
   */
  List<ExperimentFolder> findAllByOwner_Id(String ownerId);
}
