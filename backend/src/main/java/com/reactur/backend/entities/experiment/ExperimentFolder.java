package com.reactur.backend.entities.experiment;

import com.reactur.backend.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A personal collection of {@link Experiment}s created by one {@link User}, used to organize
 * experiments the user cares about.
 */
@Entity
@Table(name = "experiment_folder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperimentFolder {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  /** Strictly the user who created the folder; folders are not shared or transferable. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false)
  private User owner;

  /**
   * Many-to-many because the same experiment can be organized into multiple folders at once,
   * including folders belonging to different users (e.g. both the creator and the scientist
   * keeping it in their own personal folders).
   */
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "folder_experiments",
    joinColumns = @JoinColumn(name = "folder_id"),
    inverseJoinColumns = @JoinColumn(name = "experiment_id")
  )
  @Builder.Default
  private List<Experiment> experiments = new ArrayList<>();

  /**
   * Optimistic-locking token. Prevents a stale save from resurrecting or double-modifying a
   * folder concurrently deleted or changed by another add/remove/delete operation.
   */
  @Version
  private Long version;
}
