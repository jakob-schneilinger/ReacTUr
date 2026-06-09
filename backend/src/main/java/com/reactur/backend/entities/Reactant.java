package com.reactur.backend.entities;

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

    @Column(name = "smiles", nullable = false)
    private String smiles;

    @Column(name = "in_chi_key")
    private String inChiKey;

    @Column(name = "chemical_name")
    private String chemicalName;

    @Column(name = "cas_number")
    private String casNumber;

    @Column(name = "structure_path_xyz")
    private String structurePathXyz;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = false)
    private User createdBy;
}
