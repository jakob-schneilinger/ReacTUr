import { Injectable } from '@angular/core';
import { delay, map, Observable, of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { HttpClient } from '@angular/common/http';
import { Globals } from '../global/globals';
import { MOCK_REACTANT_DETAILS} from '../mock/mock-reactants';
import { ReactantDetailDto, ReactantDto, SaveReactantDto } from '../dtos/reactant.dto';
import { MOCK_CHEM_TYPES } from '../mock/mock.data';

@Injectable({
  providedIn: 'root',
})
export class ReactantService {
  private reactantBaseUri: string;
  private fileBaseUri: string;

  private reactantsDb: ReactantDetailDto[] = [...(MOCK_REACTANT_DETAILS as ReactantDetailDto[])];

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
    const activeReactants = this.reactantsDb.filter((r) => !r.archivedAt);
    return of(activeReactants).pipe(delay(300));

    return this.httpClient.get<ReactantDto[]>(this.reactantBaseUri);
  }

  getArchivedReactants(): Observable<ReactantDto[]> {
    const archivedReactants = this.reactantsDb.filter((r) => !!r.archivedAt);
    return of(archivedReactants).pipe(delay(300));
    return this.httpClient.get<ReactantDto[]>(`${this.reactantBaseUri}/archived`);
  }

  getReactantDetail(uuid: string): Observable<ReactantDetailDto> {
    const reactant = this.reactantsDb.find((r) => r.uuid === uuid);
    if (!reactant) {
      return throwError(() => new Error(`Reactant with UUID ${uuid} not found`));
    }
    return of(reactant).pipe(delay(300));
    return this.httpClient.get<ReactantDetailDto>(`${this.reactantBaseUri}/${uuid}`);
  }

  createReactant(saveReactant: SaveReactantDto): Observable<ReactantDetailDto> {
    const chemTypeObj = Object.values(MOCK_CHEM_TYPES).find(
      (ct) => ct.uuid === saveReactant.chemicalType,
    );

    if (!chemTypeObj) {
      return throwError(() => new Error('Invalid Chemical Type UUID'));
    }

    const newReactant: ReactantDetailDto = {
      uuid: `r-${Math.random().toString(36).substring(2, 9)}`,
      archivedAt: null,
      smiles: saveReactant.smiles,
      shortName: saveReactant.shortName,
      chemicalName: saveReactant.chemicalName,
      chemicalType: chemTypeObj,
      additionalShortNames: saveReactant.additionalShortNames || [],
      casNumber: saveReactant.casNumber,
      inChiKey: saveReactant.inChiKey,
      comment: saveReactant.comment,
      references: saveReactant.references || [],
      structures: [],
    };

    this.reactantsDb.push(newReactant);
    return of(newReactant).pipe(delay(300));
    return this.httpClient.post<ReactantDetailDto>(this.reactantBaseUri, saveReactant);
  }

  updateReactant(uuid: string, saveReactant: SaveReactantDto): Observable<ReactantDetailDto> {
    const index = this.reactantsDb.findIndex((r) => r.uuid === uuid);

    if (index === -1) {
      return throwError(() => new Error(`Reactant with UUID ${uuid} not found`));
    }

    const chemTypeObj = Object.values(MOCK_CHEM_TYPES).find(
      (ct) => ct.uuid === saveReactant.chemicalType,
    );

    this.reactantsDb[index] = {
      ...this.reactantsDb[index],
      smiles: saveReactant.smiles,
      shortName: saveReactant.shortName,
      chemicalName: saveReactant.chemicalName,
      chemicalType: chemTypeObj || this.reactantsDb[index].chemicalType,
      additionalShortNames: saveReactant.additionalShortNames || [],
      casNumber: saveReactant.casNumber,
      inChiKey: saveReactant.inChiKey,
      comment: saveReactant.comment,
      references: saveReactant.references || [],
    };

    return of(this.reactantsDb[index]).pipe(delay(300));

    return this.httpClient.patch<ReactantDetailDto>(
      `${this.reactantBaseUri}/${uuid}`,
      saveReactant,
    );
  }

  archiveReactant(uuid: string): Observable<void> {
    const reactant = this.reactantsDb.find((r) => r.uuid === uuid);
    if (reactant) {
      reactant.archivedAt = new Date().toISOString();
    }
    return of(void 0).pipe(delay(300));

    return this.httpClient.delete<void>(`${this.reactantBaseUri}/${uuid}`);
  }

  restoreReactant(uuid: string): Observable<ReactantDto> {
    const reactant = this.reactantsDb.find((r) => r.uuid === uuid);
    if (!reactant) {
      return throwError(() => new Error(`Reactant with UUID ${uuid} not found`));
    }

    reactant.archivedAt = null;
    return of(reactant).pipe(delay(300));
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

  loadReactantStructures(reactant: ReactantDetailDto | ReactantDto): Observable<File[]> {
    return this.getReactantStructures(reactant);
  }
}
