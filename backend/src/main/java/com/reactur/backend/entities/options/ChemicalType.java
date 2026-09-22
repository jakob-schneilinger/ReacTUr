package com.reactur.backend.entities.options;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The chemical classification assigned to a reactant. Shared fields defined in {@link BaseOption}.
 */
@Entity
@Table(name = "chemical_type")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ChemicalType extends BaseOption {
}
