import { CommonModule, NgOptimizedImage } from '@angular/common';
import { Component } from "@angular/core";
import { AuthService } from "../../../../services/auth.service";
import { Router, RouterModule } from '@angular/router';
import { GetUserDto } from '../../../../dtos/user.dto';
import { HasRoleDirective } from '../../../../directives/has-role.directive';
import { NgbDropdown, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle } from '@ng-bootstrap/ng-bootstrap';
import { TRANSLOCO_IMPORTS } from '../../../../global/transloco-imports';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    HasRoleDirective,
    NgOptimizedImage,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownItem,
    TRANSLOCO_IMPORTS
  ],
})
export class HeaderComponent {
  constructor(
    public router: Router,
    public authService: AuthService,
  ) {}

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get user(): GetUserDto | null {
    return this.authService.getLoggedInUser();
  }

  logout() {
    this.authService.logout();
  }
}
