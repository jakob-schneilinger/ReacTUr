package com.reactur.backend.dtos.folders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * DTO for retrieving an experiment folder and its contents.
 *
 * @param id            The unique identifier of the folder.
 * @param name          The displayed name of the folder.
 * @param experimentIds The UUIDs of the experiments contained within this folder.
 */
public record GetFolderDto(
  UUID id,
  String name,
  List<UUID> experimentIds
) {
  public GetFolderDto {
    if (experimentIds == null) {
      experimentIds = Collections.emptyList();
    }
  }
}
