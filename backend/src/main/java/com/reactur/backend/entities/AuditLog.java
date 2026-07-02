package com.reactur.backend.entities;

import com.reactur.backend.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    // TODO: Remove and address these issues, ask Dennis, etc.
    /*
        1. (For us) How are we calculating the JSON diffs?
        Because changeFrom and changeTo are custom JSON maps, ask your team: Are we calculating these differences manually in the Service layer, or are we planning to use a library like Hibernate Envers or Javers? (If manual, the updateExperiment method will be quite complex to write later!).

        2. For the Chemist Partner (Dennis): Compliance/FDA regulations?
        In chemical and pharmaceutical industries, there is a standard called FDA 21 CFR Part 11 for electronic records.

    "Do our audit logs need to capture a 'Reason for Change' (e.g., user types 'Corrected typo in concentration')? Do we need digital signatures for updates?" If yes, we will need to add a reasonForChange string column to this table later.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "experiment_id", nullable = false)
    private UUID experimentId;

    // Todo: Remove: FIXED: Changed from UUID to
    //  String to perfectly match User.java (Keycloak ID)
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default  // Default value for easier building
    private Instant timestamp = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "change_from", columnDefinition = "jsonb")
    private Map<String, Object> changeFrom;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "change_to", columnDefinition = "jsonb")
    private Map<String, Object> changeTo;
}
