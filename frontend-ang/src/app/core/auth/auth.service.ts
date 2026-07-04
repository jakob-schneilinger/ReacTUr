import { GetUserDto, RoleDto } from '../../shared/dtos/user.dto';
import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';
import { catchError, map, Observable, of } from 'rxjs';
import { UserService } from '../../shared/services/user.service';
import { Role } from '../../shared/dtos/enums/role.enum';
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

  /**
   * Starts the OAuth code flow.
   * @param targetUrl The URL to redirect to after login.
   */
  public login(targetUrl: string = this.router.url) {
    this.oauthService.initCodeFlow(targetUrl);
  }

  /**
   * Logs the user out and clears the local user cache.
   */
  public logout() {
    this.oauthService.logOut();
  }

  /**
   * Synchronously checks whether a valid access token is currently available.
   */
  public isLoggedIn(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  /**
   * The central source of truth for the current user.
   * ALWAYS waits for the initial login flow before evaluating data.
   * Fetches the user role from the backend and constructs the GetUserDto.
   */
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

  /**
   * Reactively checks whether the logged-in user possesses one of the passed roles.
   * This method is used by guards and directives.
   * @param allowedRoles Array of roles (e.g., ['ADMIN', 'SUPERVISOR'])
   * @returns Observable true if the user is authorized.
   */
  public hasRole(allowedRoles: string[]): Observable<boolean> {
    return this.getLoggedInUser().pipe(
      map((user) => {
        if (!user) {
          return false;
        }
        return allowedRoles.includes(user.role);
      })
    );
  }

  // TODO: think about function, placement, parameters.
  public hasExperimentPermission(experimentUuid: string, userUuid: string): Observable<boolean> {
    return of(false);
  }
}
