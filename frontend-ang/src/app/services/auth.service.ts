import { User } from "../dtos/user";
import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from '../auth.config';

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

  //TODO: remove
  hardcoded_user: User = {
    email: 'jakob@schneilinger.com',
    firstName: 'Jakob',
    lastName: 'Schneilinger',
    role: 'admin',
  };
  hardcoded_logged_in: boolean = true;

  public login() {
    //TODO: remove if auth working
    this.hardcoded_logged_in = true;

    this.oauthService.initCodeFlow();
  }

  public logout() {
    //TODO: remove if auth working
    this.hardcoded_logged_in = false;

    this.oauthService.logOut();
  }

  public isLoggedIn(): boolean {
    //TODO: remove if auth working
    return this.hardcoded_logged_in;

    return this.oauthService.hasValidAccessToken();
  }

  public getToken(): string {
    return this.oauthService.getAccessToken();
  }

  public getLoggedInUser(): User | null {
    //TODO: remove if auth working
    return this.hardcoded_user;

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
}
