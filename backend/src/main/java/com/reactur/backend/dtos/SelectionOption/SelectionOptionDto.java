package com.reactur.backend.dtos.SelectionOption;

import com.reactur.backend.dtos.DetailsDto;

/**
 * Interface for Options that are determined by the admin
 */
public interface SelectionOptionDto extends DetailsDto {

    /**
     * Gets the display name of the selection option.
     *
     * @return The name of the option.
     */
    String name();
}
