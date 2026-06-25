import { GetUserDto } from '../dtos/user.dto';
import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from '../auth.config';
import { Observable, of } from 'rxjs';
import { MOCK_USERS } from '../mock/mock.data';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  public initialLoginSequence: Promise<boolean>;

  hardcodedUser: GetUserDto;

  constructor(private oauthService: OAuthService) {
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
    if (!claims) return null;

    return {
      uuid: claims['id'] ?? '-1',
      firstName: claims['given_name'],
      lastName: claims['family_name'],
      email: claims['email'],
      role: claims['role'] ?? 'USER', //TODO: get from keycloak
      deactivatedAt: claims['deactivated_at'] ?? '', //TODO: get from keycloak
    };
  }

  public getUserRole(): string {
    // return this.hardcodedUser.role ?? "USER";
    if (!this.isLoggedIn()) {
      return 'UNDEFINED';
    }

    const claims: any = this.oauthService.getIdentityClaims();
    const roles: string[] = claims['realm_access']?.roles || [];

    if (roles.includes('ADMIN')) {
      return 'ADMIN';
    } else if (roles.includes('USER')) {
      return 'USER';
    }
    return 'UNDEFINED';
  }

  // TODO: think about function, placement, parameters.
  public hasExperimentPermission(experimentUuid: string, userUuid: string): Observable<boolean> {
    return of(false);
  }
}
