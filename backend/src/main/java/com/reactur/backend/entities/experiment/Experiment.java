package com.reactur.backend.entities.experiment;

import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single chemical kinetics experiment combining two {@link Reactant}s, its {@link Metadata}
 * (conditions and who performed it), and its {@link ExperimentResult}.
 */
@Entity
@Table(name = "experiment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reactant_a_id", nullable = false)
    private Reactant reactantA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reactant_b_id", nullable = false)
    private Reactant reactantB;

    /**
     * A paper can report multiple experiments, and one experiment can be published in multiple
     * papers, so this is a many-to-many list rather than a single value.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "experiment_dois", joinColumns = @JoinColumn(name = "experiment_id"))
    @Column(name = "doi")
    @Builder.Default
    private List<String> dois = new ArrayList<>();

    /** Cascades fully: exclusively owned by this experiment, never shared or reused. */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_id", referencedColumnName = "id")
    private Metadata metadata;

    /** Cascades fully: exclusively owned by this experiment, never shared or reused. */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", referencedColumnName = "id")
    private ExperimentResult result;

    /**
     * Cascades fully, so deleting the experiment deletes its attachments too. {@code
     * orphanRemoval} is declared but not currently exercised: attachments are archived via their
     * own {@code archivedAt}, never removed from this list.
     */
    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExperimentAttachment> files = new ArrayList<>();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = false)
    private User createdBy;

    /**
     * Optimistic-locking token. Without it, since no entity here uses {@code @DynamicUpdate},
     * Hibernate's default full-column {@code UPDATE} would silently revert a concurrent
     * archive/restore commit back to a stale value on every unrelated edit. {@code @DiffIgnore}
     * keeps this technical field out of the Javers audit trail, so it doesn't show up as a
     * spurious change on every edit.
     */
    @Version
    @DiffIgnore
    private Long version;
}
