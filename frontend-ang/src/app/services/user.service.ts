import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { delay, Observable, of } from 'rxjs';
import { Globals } from '../global/globals';
import { AuthService } from './auth.service';
import { GetUserDto, SetRoleDto } from '../dtos/user.dto';
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
    private authService: AuthService,
  ) {
    this.userBaseUri = this.globals.backendUri + '/users';
  }

  getAllUsers(): Observable<GetUserDto[]> {
    //TODO: remove when backend exists
    return of(Object.values(MOCK_USERS)).pipe(delay(400));
    return this.httpClient.get<GetUserDto[]>(`${this.userBaseUri}`);
  }

  // backend setRole
  updateUserRole(uuid: string, role: Role): Observable<void> {
    //TODO: remove when backend exists
    const usersArray = Object.values(MOCK_USERS);
    const user = usersArray.find((u) => u.uuid === uuid);
    if (user) {
      user.role = role;
    }
    return of(void 0).pipe(delay(300));
    const payload: SetRoleDto = { uuid, role };
    return this.httpClient.put<void>(`${this.userBaseUri}/${uuid}`, payload);
  }

  archiveUser(uuid: string): Observable<void> {
    //TODO: remove when backend exists
    const usersArray: GetUserDto[] = Object.values(MOCK_USERS);
    const user = usersArray.find((u) => u.uuid === uuid);
    if (user) {
      user.deactivatedAt = new Date().toISOString();
    }
    return of(void 0).pipe(delay(300));
    return this.httpClient.delete<void>(`${this.userBaseUri}/${uuid}`);
  }

  restoreUser(uuid: string): Observable<void> {
    //TODO: remove when backend exists
    const usersArray: GetUserDto[] = Object.values(MOCK_USERS);
    const user = usersArray.find((u) => u.uuid === uuid);
    if (user) {
      user.deactivatedAt = null;
    }
    return of(void 0).pipe(delay(300));
    return this.httpClient.put<void>(`${this.userBaseUri}/${uuid}/restore`, {});
  }
}
