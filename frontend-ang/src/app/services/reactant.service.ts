import { Injectable } from '@angular/core';
import { delay, Observable, of } from 'rxjs';
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

    const mockdata = MOCK_REACTANTS.filter(reactant => reactant.shortName.toLowerCase().includes(shortName.toLowerCase()));
    return of(mockdata);
  }

  deleteReactant(shortName: string): Observable<boolean> {

    return of(true);
  }
}
