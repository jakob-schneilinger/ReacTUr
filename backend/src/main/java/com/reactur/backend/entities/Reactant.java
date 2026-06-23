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
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reactant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("archived_at IS NULL")
public class Reactant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "short_name", nullable = false, unique = true)
    private String shortName;

    /*
    TODO, remove gemini's comment
     By default, Hibernate translates a Java String into a PostgreSQL VARCHAR(255) (maximum 255 characters).
     While an InChIKey is always exactly 27 characters, SMILES strings can be massive—especially for large polymers or complex proteins. If a chemist uploads a SMILES string with 300 characters, the database will throw a fatal truncation error.
     The Solution: We should tell Hibernate to use the TEXT datatype for SMILES and file paths, which has unlimited length. Add columnDefinition = "TEXT" to the @Column annotation for smiles and structurePathXyz.
    */

    @Column(name = "smiles", nullable = false, columnDefinition = "TEXT")
    private String smiles;

    @Column(name = "in_chi_key", unique = true)  // ToDo: Should bre unique = true?
    private String inChiKey;

    /* TODO remove and ask Dennis:
        Right now, only shortName is marked as unique = true. An InChIKey is a hashed signature explicitly designed by IUPAC to be a globally unique identifier for a specific chemical.
        Question for your chemist partner: Should inChiKey be enforced as unique = true in the database? (Usually, the answer is yes, to prevent two users from accidentally creating the exact same chemical twice under different names).
     */

    @Column(name = "chemical_name")
    private String chemicalName;

    @Column(name = "cas_number")
    private String casNumber;

    @Column(name = "structure_path_xyz", columnDefinition = "TEXT")
    private String structurePathXyz;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = false)
    private User createdBy;

    /* TODO:
    The Bug: Your DTO expects a ChemicalTypeDto.
    However, if you look back at our Reactant entity code from yesterday,
    we never actually added a ChemicalType field to it! *
    The Fix: We need to add a TODO
                               to update the Reactant entity later with a
                               @ManyToOne relationship to the ChemicalType table.
     */
}
