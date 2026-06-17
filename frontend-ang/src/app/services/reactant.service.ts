import { Injectable } from '@angular/core';
import { delay, map, Observable, of } from 'rxjs';
import { AuthService } from './auth.service';
import { HttpClient } from '@angular/common/http';
import { Globals } from '../global/globals';
import { MOCK_REACTANTS} from '../mock/mock-reactants';
import { ReactantDetailDto, ReactantDto, SaveReactantDto } from '../dtos/reactant.dto';

@Injectable({
  providedIn: 'root',
})
export class ReactantService {
  private reactantBaseUri: string;
  private fileBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
    private authService: AuthService,
  ) {
    this.reactantBaseUri = this.globals.backendUri + '/reactant';
    this.fileBaseUri = this.globals.backendUri + '/file';
  }

  getAllReactants(): Observable<ReactantDto[]> {
    // TODO: Backend-Call

    return of(MOCK_REACTANTS);
    return this.httpClient.get<ReactantDto[]>(this.reactantBaseUri);
  }

  getArchivedReactants(): Observable<ReactantDto[]> {
    return this.httpClient.get<ReactantDto[]>(`${this.reactantBaseUri}/archived`);
  }

  getReactantDetail(uuid: string): Observable<ReactantDetailDto> {
    return this.httpClient.get<ReactantDetailDto>(`${this.reactantBaseUri}/${uuid}`);
  }

  createReactant(saveReactant: SaveReactantDto): Observable<ReactantDetailDto> {
    return this.httpClient.post<ReactantDetailDto>(this.reactantBaseUri, saveReactant);
  }

  updateReactant(uuid: string, saveReactant: SaveReactantDto): Observable<ReactantDetailDto> {
    return this.httpClient.patch<ReactantDetailDto>(
      `${this.reactantBaseUri}/${uuid}`,
      saveReactant,
    );
  }

  archiveReactant(uuid: string): Observable<void> {
    return this.httpClient.delete<void>(`${this.reactantBaseUri}/${uuid}`);
  }

  restoreReactant(uuid: string): Observable<ReactantDto> {
    return this.httpClient.put<ReactantDto>(`${this.reactantBaseUri}/${uuid}/restore`, {});
  }

  getReactantStructures(reactant: ReactantDto): Observable<File[]> {
    // TODO: Backend-Call
    const url = 'mol.xyz';

    return this.httpClient
      .get(url, {
        responseType: 'blob',
      })
      .pipe(
        map((blob: Blob) => {
          const file = new File([blob], `${reactant.shortName}.xyz`, { type: 'chemical/x-xyz' });
          return [file];
        }),
      );

    return this.httpClient.get(this.fileBaseUri, { responseType: 'blob' }).pipe(
      map((blob: Blob) => {
        const file = new File([blob], `${reactant.shortName}.xyz`, { type: 'chemical/x-xyz' });
        return [file];
      }),
    );
  }
}
