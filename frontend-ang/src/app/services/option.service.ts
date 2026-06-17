import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { UserDto } from '../dtos/user.dto';
import { KnownOptionDto } from '../dtos/selection-option.dto';
import { environment } from '../../environment/environment';
import { Globals } from '../global/globals';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class OptionService {
  private optionBaseUri = `${environment.apiUrl}`;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
    private authService: AuthService,
  ) {
    // TODO: update url
    this.optionBaseUri = this.globals.backendUri;
  }

  knownOptions: KnownOptionDto[] = [
    { id: 1, category: 'type', value: 'type1', isActive: true },
    { id: 2, category: 'type', value: 'type2', isActive: true },
    { id: 3, category: 'type', value: 'type3', isActive: true },
    { id: 4, category: 'modality', value: 'modality1', isActive: true },
    { id: 5, category: 'modality', value: 'modality2', isActive: true },
    { id: 6, category: 'modality', value: 'modality3', isActive: false },
    { id: 7, category: 'instrument', value: 'instrument1', isActive: true },
    { id: 8, category: 'instrument', value: 'instrument2', isActive: false },
    { id: 9, category: 'instrument', value: 'instrument3', isActive: false },
    { id: 10, category: 'instrument', value: 'instrument4', isActive: false },
    { id: 11, category: 'solvent', value: 'solvent1', isActive: false },
    { id: 12, category: 'solvent', value: 'solvent2', isActive: false },
    { id: 13, category: 'solvent', value: 'solvent3', isActive: false },
    { id: 14, category: 'solvent', value: 'solvent4', isActive: true },
    { id: 15, category: 'solvent', value: 'solvent5', isActive: false },
  ];

  getKnownOptions(
    category: 'type' | 'modality' | 'instrument' | 'solvent',
  ): Observable<KnownOptionDto[]> {
    //TODO: remove when backend exists
    const filteredOptions = this.knownOptions.filter((opt) => opt.category === category);
    return of(filteredOptions);
    return this.httpClient.get<KnownOptionDto[]>(`${this.optionBaseUri}/options/${category}`);
  }

  addKnownOption(
    category: 'type' | 'modality' | 'instrument' | 'solvent',
    value: string,
  ): Observable<KnownOptionDto> {
    //TODO: remove when backend exists
    const option = {
      id: this.knownOptions.length + 1,
      category: category,
      value: value,
      isActive: true,
    };
    this.knownOptions.push(option);
    return of(option);
    const payload = { category, value, isActive: true };
    return this.httpClient.post<KnownOptionDto>(`${this.optionBaseUri}/options`, payload);
  }

  updateKnownOptionStatus(optionId: number, isActive: boolean): Observable<KnownOptionDto> {
    //TODO: remove when backend exists, backend needs to check if not all options deactivated
    const option = this.knownOptions.find((opt) => opt.id === optionId);
    if (!option) {
      return throwError(() => new Error(`Option with id ${optionId} not found.`));
    }

    const category = option.category;
    option.isActive = isActive;

    if (category === 'type') {
      if (this.knownOptions.filter((opt) => opt.isActive && opt.category === 'type').length === 0) {
        option.isActive = !isActive;
        return throwError(() => new Error('Type must have at least ONE active option.'));
      }
    }
    if (category === 'solvent') {
      if (
        this.knownOptions.filter((opt) => opt.isActive && opt.category === 'solvent').length === 0
      ) {
        option.isActive = !isActive;
        return throwError(() => new Error('Solvent must have at least ONE active option.'));
      }
    }

    return of(option);
    return this.httpClient.patch<KnownOptionDto>(`${this.optionBaseUri}/options/${optionId}/status`, {
      isActive,
    });
  }
}
