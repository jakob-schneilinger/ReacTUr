import { GetUserDto, RoleDto } from '../dtos/user.dto';
import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from '../auth.config';
import { catchError, from, map, Observable, of, switchMap } from 'rxjs';
import { MOCK_USERS } from '../mock/mock.data';
import { UserService } from './user.service';
import { Role } from '../dtos/enums/role.enum';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  public initialLoginSequence: Promise<boolean>;

  user: GetUserDto | null = null;

  constructor(
    private oauthService: OAuthService,
    private userService: UserService,
    private router: Router,
  ) {
    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();

    this.initialLoginSequence = this.oauthService
      .loadDiscoveryDocumentAndTryLogin()
      .then((success) => {
        if (success && this.oauthService.hasValidAccessToken()) {
          const targetUrl = this.oauthService.state;

          if (targetUrl && targetUrl !== '/search' && targetUrl !== '/') {
            setTimeout(() => {
              this.router.navigateByUrl(targetUrl);
            });
          }
        }
        return success;
      });
  }

  public login(targetUrl: string = this.router.url) {
    this.oauthService.initCodeFlow(targetUrl);
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

  public getLoggedInUser(): Observable<GetUserDto | null> {
    if (!this.isLoggedIn()) {
      return of(null);
    }

    if (this.user) {
      return of(this.user);
    }

    const claims: any = this.oauthService.getIdentityClaims();
    if (!claims || !claims['sub'] || claims['sub'] == '-1') {
      return of(null);
    }

    const id = '' + claims['sub'];

    return this.userService.getUserRole(id).pipe(
      map((roleDto: RoleDto) => {
        this.user = {
          id: id,
          firstName: claims['given_name'],
          lastName: claims['family_name'],
          email: claims['email'],
          role: roleDto.role,
          deactivatedAt: claims['deactivated_at'] ?? '',
        };
        return this.user;
      }),
      catchError((err) => {
        console.error('Failed to fetch role:', err);
        return of({
          id: id,
          firstName: claims['given_name'],
          lastName: claims['family_name'],
          email: claims['email'],
          role: Role.UNAUTHORIZED,
          deactivatedAt: claims['deactivated_at'] ?? '',
        });
      }),
    );
  }

  public getUserRole(): Role {
    // return this.hardcodedUser.role ?? "USER";
    if (!this.isLoggedIn()) {
      return Role.UNAUTHORIZED;
    }

    if (this.user) {
      return this.user.role;
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

  public checkRoleReactive(allowedRoles: string[]): Observable<boolean> {
    return from(this.initialLoginSequence).pipe(
      switchMap(() => {
        if (!this.isLoggedIn()) {
          return of(false);
        }

        return this.getLoggedInUser().pipe(
          map((user) => {
            let currentRole = Role.UNAUTHORIZED;

            if (user) {
              currentRole = user.role;
            } else {
              const claims: any = this.oauthService.getIdentityClaims();
              const roles: string[] = claims?.['realm_access']?.roles || [];
              if (roles.includes('ADMIN')) currentRole = Role.ADMIN;
              else if (roles.includes('SUPERVISOR')) currentRole = Role.SUPERVISOR;
              else if (roles.includes('USER')) currentRole = Role.USER;
            }

            console.log('Evaluated reactive role:', currentRole);
            return allowedRoles.includes(currentRole);
          }),
        );
      }),
    );
  }
}
