package com.reactur.backend.entities.options;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "instrument")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// TODO: ArchivedAt instead of isActive
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    /* TODO: Remove this comment, but it justifies why I added this menu here thanks to Gemini's analysis.
        I realize there is only one instrument right now? But, if not complicated, I'm guessing we can call
        this "more scalable"
    The "Broken Instrument" Problem (Architectural Question): What happens if a piece of equipment breaks and is thrown away, or you stop buying a certain solvent?

    If you completely delete the Instrument from the database, all past Experiments that used it will crash or lose their data.

    If you leave it as is, scientists in 2027 will still see a broken 2023 instrument in their dropdown menu.

    The Solution: We should add a soft-delete mechanism to these lookups. A simple @Column(name = "is_active") private Boolean isActive = true; allows you to hide old instruments from the frontend dropdowns without destroying historical data.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default // Tells Lombok to use this default value when building
    private Boolean isActive = true;

}
