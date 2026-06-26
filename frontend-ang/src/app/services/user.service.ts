import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { delay, Observable, of } from 'rxjs';
import { Globals } from '../global/globals';
import { AuthService } from './auth.service';
import { GetUserDto, RoleDto } from '../dtos/user.dto';
import { Role } from '../dtos/enums/role.enum';
import { MOCK_USERS } from '../mock/mock.data';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private userBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
  ) {
    this.userBaseUri = this.globals.backendUri + '/users';
  }

  getAllUsers(): Observable<GetUserDto[]> {
    return this.httpClient.get<GetUserDto[]>(`${this.userBaseUri}`);
  }

  // backend setRole
  updateUserRole(uuid: string, role: Role): Observable<void> {
    const payload: RoleDto = { role };
    return this.httpClient.put<void>(`${this.userBaseUri}/${uuid}`, payload);
  }

  // backend getRole
  getUserRole(uuid: string): Observable<RoleDto> {
    return this.httpClient.get<RoleDto>(`${this.userBaseUri}/${uuid}/role`);
  }

  archiveUser(uuid: string): Observable<void> {
    return this.httpClient.delete<void>(`${this.userBaseUri}/${uuid}`);
  }

  restoreUser(uuid: string): Observable<void> {
    return this.httpClient.put<void>(`${this.userBaseUri}/${uuid}/restore`, {});
  }
}
