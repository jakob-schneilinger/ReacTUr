import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

@Injectable({
  providedIn: 'root',
})
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService,
  ) {}

  async canActivate(route: ActivatedRouteSnapshot): Promise<boolean> {
    await this.authService.initialLoginSequence;

    const expectedRoles = route.data['roles'] as Array<string>;
    const userRole = this.authService.getUserRole();

    console.log("User role: ", userRole);

    if (!expectedRoles.includes(userRole)) {
      this.notification.error("Access Denied", "You don't have the required permissions to access this page.")
      this.router.navigate(['/search']);
      return false;
    }
    return true;
  }
}
