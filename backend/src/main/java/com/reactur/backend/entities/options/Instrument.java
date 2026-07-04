package com.reactur.backend.entities.options;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "instrument")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Instrument extends BaseOption {
}
