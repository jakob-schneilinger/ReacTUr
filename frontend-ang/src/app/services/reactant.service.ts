import { Injectable } from '@angular/core';
import { delay, map, Observable, of } from 'rxjs';
import { AuthService } from './auth.service';
import { HttpClient } from '@angular/common/http';
import { Globals } from '../global/globals';
import { MOCK_REACTANTS} from '../mock/mock-reactants';
import { Reactant } from '../dtos/reactant';

@Injectable({
  providedIn: 'root',
})
export class ReactantService {
  private userBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
    private authService: AuthService,
  ) {
    this.userBaseUri = this.globals.backendUri + '/reactant';
  }

  getAllReactants(): Observable<Reactant[]> {
    // TODO: Backend-Call

    return of(MOCK_REACTANTS);
  }

  getReactants(shortName: string): Observable<Reactant[]> {
    // TODO: Backend-Call

    const mockdata = MOCK_REACTANTS.filter((reactant) =>
      reactant.shortName.toLowerCase().includes(shortName.toLowerCase()),
    );
    return of(mockdata);
  }

  getReactant(shortName: string): Observable<Reactant> {
    // TODO: Backend-Call

    const mockdata = MOCK_REACTANTS.filter((reactant) =>
      reactant.shortName.toLowerCase() === shortName.toLowerCase(),
    )[0];
    return of(mockdata);
  }

  deleteReactant(shortName: string): Observable<boolean> {
    // TODO: Backend-Call

    return of(true);
  }

  getReactantStructures(shortName: string): Observable<File[]> {
    // TODO: Backend-Call
    const url = 'mol.xyz';

    return this.httpClient
      .get(url, {
        responseType: 'blob',
      })
      .pipe(
        map((blob: Blob) => {
          const file = new File([blob], `${shortName}.xyz`, { type: 'chemical/x-xyz' });
          return [file];
        }),
      );

    return of([])
  }
}
