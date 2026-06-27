import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService,
  ) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const expectedRoles = route.data['roles'] as Array<string>;

    return this.authService.checkRoleReactive(expectedRoles).pipe(
      tap((hasAccess: boolean) => {
        if (!hasAccess) {
          this.notification.error(
            'Access Denied',
            "You don't have the required permissions to access this page.",
          );
          this.router.navigate(['/search']);
        }
      }),
    );
  }
}
