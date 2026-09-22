package com.reactur.backend.entities.options;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The instrument used to record an experiment's measurement. Shared fields defined in {@link
 * BaseOption}.
 */
@Entity
@Table(name = "instrument")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Instrument extends BaseOption {
}
