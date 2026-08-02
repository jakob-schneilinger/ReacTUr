package com.reactur.backend.dtos.folders;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating an experiment folder.
 *
 * @param name The displayed name of the folder.
 */
public record SaveFolderDto(
  @NotBlank(message = "Folder name cannot be empty")
  @Size(max = 255, message = "Folder name must not exceed 255 characters")
  String name
) {
}
