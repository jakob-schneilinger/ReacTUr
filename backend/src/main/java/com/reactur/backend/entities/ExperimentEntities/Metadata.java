package com.reactur.backend.entities.ExperimentEntities;

import com.reactur.backend.entities.SelectionOptions.Instrument;
import com.reactur.backend.entities.SelectionOptions.Solvent;
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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "scientist", nullable = false)
    private String scientist;

    @Column(name = "temperature")
    private Float temperature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solvent_id")
    private Solvent solvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @Column(name = "concentration_a")
    private Float concentrationA;

    @Column(name = "concentration_b")
    private Float concentrationB;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
