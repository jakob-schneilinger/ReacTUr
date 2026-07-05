import {
  MOCK_CHEM_TYPES,
  MOCK_SOLVENTS,
  MOCK_INSTRUMENTS,
  MOCK_MODALITIES
} from '../../mock/mock.data';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Globals } from '../../core/global/globals';
import { AuthService } from '../../core/auth/auth.service';
import { delay, Observable, of, throwError } from 'rxjs';

export interface BaseOptionDto {
  uuid: string;
  name: string;
  archivedAt: string | null;
}

export type OptionCategory = 'chemical_type' | 'modality' | 'instrument' | 'solvent';

@Injectable({
  providedIn: 'root',
})
export class OptionService {
  private optionBaseUri: string;

  private mockData: Record<OptionCategory, BaseOptionDto[]> = {
    chemical_type: Object.values(MOCK_CHEM_TYPES),
    solvent: Object.values(MOCK_SOLVENTS),
    instrument: Object.values(MOCK_INSTRUMENTS),
    modality: Object.values(MOCK_MODALITIES),
  };

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
  ) {
    this.optionBaseUri = this.globals.backendUri;
  }

  getSelectionOptions(category: OptionCategory): Observable<BaseOptionDto[]> {
    return this.httpClient.get<BaseOptionDto[]>(`${this.optionBaseUri}/${category}`);
  }

  // createSelectionOption in backend
  addSelectionOption(category: OptionCategory, name: string): Observable<BaseOptionDto> {
    return this.httpClient.post<BaseOptionDto>(`${this.optionBaseUri}/${category}`, { name });
  }

  updateSelectionOptionStatus(category: OptionCategory, uuid: string, isActive: boolean): Observable<void> {
    if (isActive) {
      return this.httpClient.put<void>(`${this.optionBaseUri}/${category}/${uuid}/restore`, {});
    } else {
      return this.httpClient.delete<void>(`${this.optionBaseUri}/${category}/${uuid}`);
    }
  }
}
