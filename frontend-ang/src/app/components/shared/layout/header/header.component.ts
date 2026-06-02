import { Component } from "@angular/core";
import { AuthService } from "../../../../services/auth.service";
import { Router } from "@angular/router";
import { User } from '../../../../dtos/user';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  standalone: false,
})
export class HeaderComponent {

  constructor(
    public router: Router,
    public authService: AuthService,
  ) {}

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get user(): User | null {
    return this.authService.getLoggedInUser();
  }

  logout() {
    this.authService.logout();
  }
}
