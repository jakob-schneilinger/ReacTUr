package com.reactur.backend.entities.experiment;

import com.reactur.backend.entities.options.Instrument;
import com.reactur.backend.entities.options.Modality;
import com.reactur.backend.entities.options.Solvent;
import com.reactur.backend.entities.User;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("archived_at IS NULL")
public class Metadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scientist_id", nullable = false)
    private User scientist;

    @Column(name = "temperature")
    private Double temperature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solvent_id")
    private Solvent solvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @Column(name = "concentration_a")
    private Double concentrationA;

    @Column(name = "concentration_b")
    private Double concentrationB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modality_id")
    private Modality modality;

    @Column(name = "wavelength")
    private Double wavelength;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "archived_at")
    private Instant archivedAt;
}
