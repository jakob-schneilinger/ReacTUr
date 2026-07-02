import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard {
  constructor(private authService: AuthService, private router: Router) {}

  async canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    // return true;
    await this.authService.initialLoginSequence;
    if (this.authService.isLoggedIn()) {
      return true; // User darf auf /home
    } else {
      this.authService.login(state.url);
      return false;
    }
  }
}
