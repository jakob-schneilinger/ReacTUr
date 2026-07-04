package com.reactur.backend.dtos.options;

import com.reactur.backend.dtos.DetailsDto;

/**
 * Interface for Options that are determined by the admin
 */
public interface OptionDto extends DetailsDto {

    /**
     * Gets the display name of the selection option.
     *
     * @return The name of the option.
     */
    String name();
}
