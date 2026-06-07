import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { User } from '../dtos/user';
import { KnownOption } from '../dtos/known-option';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private adminApiUrl = `${environment.apiUrl}/admin`;

  users: User[] = [
    {
      id: '1',
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice@example.com',
      role: 'USER',
      status: 'PENDING',
    },
    {
      id: '2',
      firstName: 'Bob',
      lastName: 'Jones',
      email: 'bob@example.com',
      role: 'USER',
      status: 'ACTIVATED',
    },
    {
      id: '3',
      firstName: 'Charlie',
      lastName: 'Brown',
      email: 'charlie@example.com',
      role: 'SUPERVISOR',
      status: 'ACTIVATED',
    },
    {
      id: '4',
      firstName: 'Diana',
      lastName: 'Prince',
      email: 'diana@example.com',
      role: 'USER',
      status: 'DEACTIVATED',
    },
  ];
  knownOptions: KnownOption[] = [
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

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<User[]> {
    //TODO: remove when backend exists
    return of(this.users);
    return this.http.get<User[]>(`${this.adminApiUrl}/users`);
  }

  updateUserStatus(
    userId: string,
    status: 'PENDING' | 'ACTIVATED' | 'DEACTIVATED',
  ): Observable<User> {
    //TODO: remove when backend exists
    const user = this.users.filter((user) => user.id == userId)[0];
    user['status'] = status;
    return of(user);
    return this.http.patch<User>(`${this.adminApiUrl}/users/${userId}/status`, { status });
  }

  updateUserRole(userId: string, role: 'USER' | 'SUPERVISOR' | 'ADMIN'): Observable<User> {
    //TODO: remove when backend exists
    const user = this.users.filter((user) => user.id == userId)[0];
    user['role'] = role;
    return of(user);
    return this.http.patch<User>(`${this.adminApiUrl}/users/${userId}/role`, { role });
  }

  getKnownOptions(
    category: 'type' | 'modality' | 'instrument' | 'solvent',
  ): Observable<KnownOption[]> {
    //TODO: remove when backend exists
    const filteredOptions = this.knownOptions.filter((opt) => opt.category === category);
    return of(filteredOptions);
    return this.http.get<KnownOption[]>(`${this.adminApiUrl}/options/${category}`);
  }

  addKnownOption(
    category: 'type' | 'modality' | 'instrument' | 'solvent',
    value: string,
  ): Observable<KnownOption> {
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
    return this.http.post<KnownOption>(`${this.adminApiUrl}/options`, payload);
  }

  updateKnownOptionStatus(optionId: number, isActive: boolean): Observable<KnownOption> {
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
      if (this.knownOptions.filter((opt) => opt.isActive && opt.category === 'solvent').length === 0) {
        option.isActive = !isActive;
        return throwError(() => new Error('Solvent must have at least ONE active option.'));
      }
    }

    return of(option);
    return this.http.patch<KnownOption>(`${this.adminApiUrl}/options/${optionId}/status`, {
      isActive,
    });
  }
}
