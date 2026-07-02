import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StructureDto } from './reactant.dto';
import { Globals } from '../../core/global/globals';

@Injectable({
  providedIn: 'root',
})
export class StructureService {
  private readonly structureBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
  ) {
    this.structureBaseUri = this.globals.backendUri + '/structure';
  }

  /**
   * Uploads a structure file (e.g., .xyz) for a specific reactant.
   * The status of the uploaded structure is set to ACTIVE by default.
   *
   * @param reactantId The UUID of the reactant.
   * @param file The file to be uploaded (multipart).
   * @returns An Observable containing the created StructureDto.
   */
  uploadStructure(reactantId: string, file: File): Observable<StructureDto> {
    const formData: FormData = new FormData();
    formData.append('file', file, file.name);

    return this.httpClient.post<StructureDto>(`${this.structureBaseUri}/${reactantId}`, formData);
  }

  /**
   * Triggers the automatic generation of a 3D structure via an external API.
   * The process runs in the background, and the initial status of the structure is PENDING.
   *
   * @param reactantId The UUID of the reactant for which the structure should be generated.
   * @returns An empty Observable (204 No Content).
   */
  generateStructureFromApi(reactantId: string): Observable<void> {
    return this.httpClient.post<void>(`${this.structureBaseUri}/${reactantId}/generate`, null);
  }

  /**
   * Selects a specific structure to be the active (ACTIVE) structure for the reactant.
   * Previously active structures will be set to REJECTED.
   *
   * @param structureId The UUID of the structure to select.
   * @returns An Observable containing the updated StructureDto.
   */
  selectStructure(structureId: string): Observable<StructureDto> {
    return this.httpClient.post<StructureDto>(
      `${this.structureBaseUri}/${structureId}/select`,
      null,
    );
  }

  /**
   * Converts a StructureDto from the backend into a JavaScript File object.
   * Since generated structures often do not receive a filename from the backend (name === null),
   * a fallback name is automatically assigned to prevent crashes in the 3D viewer.
   *
   * @param structure The StructureDto containing the 3D coordinates as a string.
   * @returns A File object for the 3D viewer, or null if no structure data is available.
   */
  parseFile(structure: StructureDto): File | null {
    if (structure && structure.structure) {
      const fileBits = [structure.structure];

      const fileName = structure.name || 'generated-structure.xyz';

      const options = {
        type: 'text/plain;charset=utf-8',
        lastModified: Date.now(),
      };

      return new File(fileBits, fileName, options);
    }
    return null;
  }
}
