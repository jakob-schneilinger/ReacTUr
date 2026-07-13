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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @ManyToOne(fetch = FetchType.LAZY)  // ToDo: Review LAZY load
    @JoinColumn(name = "reactant_a_id", nullable = false)
    private Reactant reactantA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reactant_b_id", nullable = false)
    private Reactant reactantB;

    // Multiple experiments could correspond to the same paper, so multiple experiments can have the same DOI
    // the same way, an experiment can appear in multiple papers and have multiple DOIs
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "experiment_dois", joinColumns = @JoinColumn(name = "experiment_id"))
    @Column(name = "doi")
    @Builder.Default
    private List<String> dois = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_id", referencedColumnName = "id")
    private Metadata metadata;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", referencedColumnName = "id")
    private ExperimentResult result;

    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExperimentAttachment> files = new ArrayList<>();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = false)
    private User createdBy;
}
