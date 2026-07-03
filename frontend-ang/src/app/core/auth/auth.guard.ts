import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { from, map, Observable, of, switchMap } from 'rxjs';
import { Role } from '../../shared/dtos/enums/role.enum';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard {
  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return from(this.authService.initialLoginSequence).pipe(
      switchMap(() => {
        if (!this.authService.isLoggedIn()) {
          this.authService.login(state.url);
          return of(false);
        }

        return this.authService.getLoggedInUser().pipe(
          switchMap((user) => {
            return this.authService.hasRole(['UNAUTHORIZED']).pipe(
              map((unauthorized) => {
                const isGoingToUnauthorized = state.url.includes('/unauthorized');

                if (unauthorized && !isGoingToUnauthorized) {
                  this.router.navigate(['/unauthorized']);
                  return false;
                }

                if (!unauthorized && isGoingToUnauthorized) {
                  this.router.navigate(['/search']);
                  return false;
                }
                return true;
              }),
            );
          }),
        );
      }),
    );
  }
}
