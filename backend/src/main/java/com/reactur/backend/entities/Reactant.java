package com.reactur.backend.entities;

import com.reactur.backend.entities.options.ChemicalType;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "reactant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reactant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "short_name", nullable = false, unique = true)
    private String shortName;

    @Column(name = "smiles", nullable = false, columnDefinition = "TEXT")
    private String smiles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    private ChemicalType type;

    @Column(name = "in_chi_key")
    private String inChiKey;

    @Column(name = "chemical_name")
    private String chemicalName;

    @Column(name = "cas_number")
    private String casNumber;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "reactant_additional_short_names", joinColumns = @JoinColumn(name = "reactant_id"))
    @Column(name = "additional_short_name")
    @Builder.Default
    private List<String> additionalShortNames = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "reactant_references", joinColumns = @JoinColumn(name = "reactant_id"))
    @Column(name = "reference")
    @Builder.Default
    private List<String> references = new ArrayList<>();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = false)
    private User createdBy;

    // Optimistic locking: see ReactantServiceImpl.updateReactant and docs/Software_todo.md's "Race condition
    // inventory" item. Without this, two concurrent updateReactant calls on the same reactant (editing different
    // fields) could silently lose one edit - whichever save() commits last would overwrite the other's changes.
    // With @Version, Hibernate includes "AND version = ?" in every UPDATE and throws
    // ObjectOptimisticLockingFailureException (mapped to HTTP 409 by GlobalExceptionHandler) if the row was already
    // changed by someone else since it was read, instead of silently overwriting it.
    @Version
    private Long version;
}
