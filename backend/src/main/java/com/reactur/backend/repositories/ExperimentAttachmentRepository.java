package com.reactur.backend.repositories;

import com.reactur.backend.entities.ExperimentEntities.ExperimentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExperimentAttachmentRepository extends JpaRepository<ExperimentAttachment, UUID> {

    List<ExperimentAttachment> findByExperimentIdAndArchivedAtIsNull(UUID experimentId);
}
