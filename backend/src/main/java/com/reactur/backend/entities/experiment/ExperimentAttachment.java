package com.reactur.backend.entities.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A file (e.g. raw measurement data) uploaded and attached to an {@link Experiment}.
 */
@Entity
@Table(name = "experiment_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperimentAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    /** The object key this file's bytes are stored under in the S3/SeaweedFS backend, not a local path. */
    @Column(name = "storage_reference_id", nullable = false)
    private String storageReferenceId;

    @Column(name = "checksum_sha256")
    private String checksumSha256;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "archived_at")
    private Instant archivedAt;
}
