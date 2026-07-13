package com.reactur.backend.dtos.folders;


import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating or updating an experiment folder.
 *
 * @param name The displayed name of the folder.
 */
public record SaveFolderDto(
  @NotBlank(message = "Folder name cannot be empty")
  String name
) {
}
