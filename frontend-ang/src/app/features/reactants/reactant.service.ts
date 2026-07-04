import { Injectable } from '@angular/core';
import { map, Observable, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { HttpClient } from '@angular/common/http';
import { Globals } from '../../core/global/globals';
import { MOCK_REACTANT_DETAILS } from '../../mock/mock-reactants';
import {
  ReactantDetailDto,
  ReactantDto,
  SaveReactantDto,
} from './reactant.dto';

@Injectable({
  providedIn: 'root',
})
export class ReactantService {
  private reactantBaseUri: string;

  private reactantsDb: ReactantDetailDto[] = [...(MOCK_REACTANT_DETAILS as ReactantDetailDto[])];

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
  ) {
    this.reactantBaseUri = this.globals.backendUri + '/reactant';
  }

  /**
   * Retrieves a list of all active reactants.
   *
   * @returns An Observable containing an array of basic ReactantDto.
   */
  getAllReactants(): Observable<ReactantDto[]> {
    return this.httpClient.get<ReactantDto[]>(this.reactantBaseUri);
  }

  /**
   * Retrieves a list of all archived reactants.
   * Requires ADMIN role.
   *
   * @returns An Observable containing an array of archived ReactantDto.
   */
  getArchivedReactants(): Observable<ReactantDto[]> {
    return this.httpClient.get<ReactantDto[]>(`${this.reactantBaseUri}/archived`);
  }

  /**
   * Retrieves the comprehensive details of a specific reactant by its UUID.
   *
   * @param uuid The unique identifier of the reactant.
   * @returns An Observable containing the ReactantDetailDto.
   */
  getReactantDetail(uuid: string): Observable<ReactantDetailDto> {
    return this.httpClient.get<ReactantDetailDto>(`${this.reactantBaseUri}/${uuid}`);
  }

  /**
   * Creates a new reactant in the database.
   * Requires ADMIN role.
   *
   * @param saveReactant The DTO containing the data for the new reactant.
   * @returns An Observable containing the newly created ReactantDetailDto.
   */
  createReactant(saveReactant: SaveReactantDto): Observable<ReactantDetailDto> {
    return this.httpClient.post<ReactantDetailDto>(this.reactantBaseUri, saveReactant);
  }

  /**
   * Updates an existing reactant.
   * Uses PATCH, so partial updates are supported. Requires ADMIN role.
   *
   * @param uuid The unique identifier of the reactant to update.
   * @param saveReactant The DTO containing the updated data (can be partial).
   * @returns An Observable containing the updated ReactantDetailDto.
   */
  updateReactant(
    uuid: string,
    saveReactant: Partial<SaveReactantDto>,
  ): Observable<ReactantDetailDto> {
    return this.httpClient.patch<ReactantDetailDto>(
      `${this.reactantBaseUri}/${uuid}`,
      saveReactant,
    );
  }

  /**
   * Archives a reactant by setting the archivedAt timestamp.
   * Requires ADMIN role in the backend.
   *
   * @param uuid The unique identifier of the reactant to archive.
   * @returns An empty Observable (204 No Content).
   */
  archiveReactant(uuid: string): Observable<void> {
    return this.httpClient.delete<void>(`${this.reactantBaseUri}/${uuid}`);
  }

  /**
   * Restores an archived reactant, making it active again.
   * Requires ADMIN role in the backend.
   *
   * @param uuid The unique identifier of the reactant to restore.
   * @returns An Observable containing the restored ReactantDto.
   */
  restoreReactant(uuid: string): Observable<ReactantDto> {
    return this.httpClient.put<ReactantDto>(`${this.reactantBaseUri}/${uuid}/restore`, {});
  }
}
