import { User } from "../dtos/user";
import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from '../auth.config';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  public initialLoginSequence: Promise<boolean>;

  constructor(private oauthService: OAuthService) {
    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();

    this.initialLoginSequence = this.oauthService.loadDiscoveryDocumentAndTryLogin();
  }

  hardcodedUser: User = {
    firstName: 'Jakob',
    lastName: 'Schneilinger',
    email: 'jakob@schneilinger.com',
    role: 'ADMIN',
  };

  public login() {
    this.oauthService.initCodeFlow();
  }

  public logout() {
    this.oauthService.logOut();
  }

  public isLoggedIn(): boolean {
    return true;
    return this.oauthService.hasValidAccessToken();
  }

  public getToken(): string {
    return this.oauthService.getAccessToken();
  }

  public getLoggedInUser(): User | null {
    return this.hardcodedUser;
    if (!this.isLoggedIn()) {
      return null;
    }
    const claims: any = this.oauthService.getIdentityClaims();
    if (!claims) return null;

    return {
      firstName: claims['given_name'],
      lastName: claims['family_name'],
      email: claims['email'],
    };
  }

  public getUserRole(): string {
    return this.hardcodedUser.role ?? "USER";
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
  public hasExperimentPermission(id: number, email: string): Observable<boolean> {
    return of(false)
  }
}
