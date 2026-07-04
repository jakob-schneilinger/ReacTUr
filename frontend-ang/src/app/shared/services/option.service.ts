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
    private authService: AuthService,
  ) {
    this.optionBaseUri = this.globals.backendUri;
  }

  getSelectionOptions(category: OptionCategory): Observable<BaseOptionDto[]> {
    return of([...this.mockData[category]]).pipe(delay(200));

    // TODO: Restore when backend exists
    return this.httpClient.get<BaseOptionDto[]>(`${this.optionBaseUri}/options/${category}`);
  }

  addSelectionOption(category: OptionCategory, name: string): Observable<BaseOptionDto> {
    const newOption: BaseOptionDto = {
      uuid: `${category}-${Math.random().toString(36).substring(2, 9)}`,
      name: name,
      archivedAt: null,
    };

    this.mockData[category].push(newOption);
    return of({ ...newOption }).pipe(delay(200));

    // TODO: Restore when backend exists
    const payload = { category, name, archivedAt: null };
    return this.httpClient.post<BaseOptionDto>(`${this.optionBaseUri}/options`, payload);
  }

  updateSelectionOptionStatus(uuid: string, isActive: boolean): Observable<BaseOptionDto> {
    let foundCategory: OptionCategory | null = null;
    let targetOption: BaseOptionDto | undefined;

    for (const category of Object.keys(this.mockData) as OptionCategory[]) {
      targetOption = this.mockData[category].find((opt) => opt.uuid === uuid);
      if (targetOption) {
        foundCategory = category;
        break;
      }
    }

    if (!targetOption || !foundCategory) {
      return throwError(() => new Error(`Option with uuid ${uuid} not found.`));
    }

    if (!isActive && (foundCategory === 'chemical_type' || foundCategory === 'solvent')) {
      const activeItemsCount = this.mockData[foundCategory].filter(
        (opt) => opt.archivedAt === null && opt.uuid !== uuid,
      ).length;

      if (activeItemsCount === 0) {
        return throwError(
          () => new Error(`${foundCategory.toUpperCase()} must have at least ONE active option.`),
        );
      }
    }

    targetOption.archivedAt = isActive ? null : new Date().toISOString();

    return of({ ...targetOption }).pipe(delay(200));

    // TODO: Restore when backend exists
    return this.httpClient.patch<BaseOptionDto>(`${this.optionBaseUri}/options/${uuid}/status`, {
      isActive,
    });
  }
}
