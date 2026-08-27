package com.reactur.backend.entities;

import com.reactur.backend.enums.StructureFlag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.util.UUID;

@Entity
@Table(name = "reactant_structure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactantStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reactant_id", nullable = false)
    private Reactant reactant;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "structure", columnDefinition = "TEXT")
    private String structure;

    @Column(name = "external_job_id")
    private String externalJobId ;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag")
    private StructureFlag flag;

    // Optimistic locking: see StructureServiceImpl.selectStructure and fetchPendingStructuresFromAPI, and
    // Software_todo.md's Race condition inventory item. Protects against a stale save() overwriting a row that a
    // concurrent call already deleted or changed (e.g. uploadStructure's deleteAll, or another selectStructure
    // call), turning a silent overwrite into a loud ObjectOptimisticLockingFailureException instead.
    @Version
    private Long version;
}
