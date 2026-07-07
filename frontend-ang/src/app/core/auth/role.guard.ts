import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { NotificationService } from '../layout/notification/notification.service';
import { Observable, tap } from 'rxjs';
import { TranslocoService } from '@jsverse/transloco';

@Injectable({
  providedIn: 'root',
})
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService,
    private transloco: TranslocoService
  ) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const expectedRoles = route.data['roles'] as Array<string>;

    return this.authService.hasRole(expectedRoles).pipe(
      tap((hasAccess: boolean) => {
        if (!hasAccess) {
          this.notification.error(
            this.transloco.translate('service.notifications.accessErrorTitle'),
            this.transloco.translate('service.notifications.accessErrorMessage'),
          );

          this.authService.hasRole(['UNAUTHORIZED']).subscribe({
            next: (value) => {
              if (value) {
                this.router.navigate(['/unauthorized']);
              } else {
                this.router.navigate(['/search']);
              }
            },
          });
        }
      }),
    );
  }
}
