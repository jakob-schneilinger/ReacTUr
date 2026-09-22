package com.reactur.backend.entities.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.util.UUID;

/**
 * The measured kinetics outcome of an {@link Experiment}: its rate constant and the associated
 * error.
 */
@Entity
@Table(name = "experiment_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperimentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rate_constant", nullable = false)
    private Double rateConstant;

    /** Null unless the experiment was also fit to a pseudo-first-order (PFO) model. */
    @Column(name = "pfo_rate_constant")
    private Double pfoRateConstant;

    @Column(name = "error_value", nullable = false)
    private Double errorValue;

    @Column(name = "is_error_percentage", nullable = false)
    private Boolean isErrorPercentage;
}
