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

  // Owner is strictly the user who created the folder.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false)
  private User owner;

  // ManyToMany allows an experiment to exist in multiple folders
  // (if Creator and Scientist both want it in their own personal folders).
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "folder_experiments",
    joinColumns = @JoinColumn(name = "folder_id"),
    inverseJoinColumns = @JoinColumn(name = "experiment_id")
  )
  @Builder.Default
  private List<Experiment> experiments = new ArrayList<>();

  // Optimistic locking prevents a stale save from resurrecting or double-modifying a concurrently
  // deleted/changed folder.
  @Version
  private Long version;
}
