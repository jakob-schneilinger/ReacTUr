import { GetUserDto, RoleDto } from '../dtos/user.dto';
import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from '../auth.config';
import { Observable, of } from 'rxjs';
import { MOCK_USERS } from '../mock/mock.data';
import { UserService } from './user.service';
import { Role } from '../dtos/enums/role.enum';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  public initialLoginSequence: Promise<boolean>;

  hardcodedUser: GetUserDto;

  constructor(
    private oauthService: OAuthService,
    private userService: UserService,
  ) {
    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();

    this.initialLoginSequence = this.oauthService.loadDiscoveryDocumentAndTryLogin();

    this.hardcodedUser = MOCK_USERS.CURIE;
  }

  public login() {
    this.oauthService.initCodeFlow();
  }

  public logout() {
    this.oauthService.logOut();
  }

  public isLoggedIn(): boolean {
    // return true;
    return this.oauthService.hasValidAccessToken();
  }

  public getToken(): string {
    return this.oauthService.getAccessToken();
  }

  public getLoggedInUser(): GetUserDto | null {
    // return this.hardcodedUser;
    if (!this.isLoggedIn()) {
      return null;
    }
    const claims: any = this.oauthService.getIdentityClaims();

    console.log(claims);
    if (!claims) return null;

    const id = claims['jti'];

    console.log(id)

    if (id == '-1' || id == null) return null;

    let role: RoleDto = {role: Role.UNAUTHORIZED};

    this.userService.getUserRole(id).subscribe({
      next: (value) => {
        role = value;
      },
      error: (err) => {
        console.error(err);
      },
    });
    return {
      uuid: id,
      firstName: claims['given_name'],
      lastName: claims['family_name'],
      email: claims['email'],
      role: role.role,
      deactivatedAt: claims['deactivated_at'] ?? '',
    };
  }

  public getUserRole(): Role {
    // return this.hardcodedUser.role ?? "USER";
    if (!this.isLoggedIn()) {
      return Role.UNAUTHORIZED;
    }

    const claims: any = this.oauthService.getIdentityClaims();
    const roles: string[] = claims['realm_access']?.roles || [];

    if (roles.includes('ADMIN')) {
      return Role.ADMIN;
    } else if (roles.includes('SUPERVISOR')) {
      return Role.SUPERVISOR;
    } else if (roles.includes('USER')) {
      return Role.USER;
    }
    return Role.UNAUTHORIZED;
  }

  // TODO: think about function, placement, parameters.
  public hasExperimentPermission(experimentUuid: string, userUuid: string): Observable<boolean> {
    return of(false);
  }
}
