package com.reactur.backend.entities.ExperimentEntities;

import com.reactur.backend.entities.Reactant;
import com.reactur.backend.entities.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import org.hibernate.annotations.SQLRestriction;

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
@SQLRestriction("archived_at IS NULL")
public class Experiment {

    // TODO: Questions for Dennis
    /*
    The "Two Reactant" Rule: Right now, the database strictly mandates exactly two reactants (reactantA and reactantB). What happens if a scientist performs a decomposition reaction (only 1 reactant)? Or a multi-component synthesis (3 or 4 reactants)?

    If yes: We might need to change this to a @ManyToMany list of reactants rather than two hardcoded columns.

    The DOI timing: Is the DOI (Digital Object Identifier) known the moment the experiment is created, or is it usually added weeks later after a paper is published?

    Currently: The column is implicitly nullable (which is good), but we should ensure the frontend knows it shouldn't force the user to type a DOI to hit "Save".
     */


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)  // ToDo: Review LAZY load
    @JoinColumn(name = "reactant_a_id", nullable = false)
    private Reactant reactantA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reactant_b_id", nullable = false)
    private Reactant reactantB;

    // ToDo: Is the doi nullable, can it be added later and is not forced to
    //  be since the beginning, or should we add not-null and always from the
    //  beginning its here? Update DTOs and FrontEnd accordingly
    @Column(name = "doi", unique = true)
    private String doi;

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
