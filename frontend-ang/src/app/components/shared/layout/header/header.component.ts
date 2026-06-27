import { CommonModule, NgOptimizedImage } from '@angular/common';
import { Component, OnInit } from '@angular/core';
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
    TRANSLOCO_IMPORTS,
  ],
})
export class HeaderComponent implements OnInit {
  constructor(
    public router: Router,
    public authService: AuthService,
  ) {}

  loggedInUser: GetUserDto | null = null;

  ngOnInit(): void {
    this.authService.initialLoginSequence.then(() => {
      this.authService.getLoggedInUser().subscribe((user) => {
        this.loggedInUser = user;
      });
    });
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get user(): GetUserDto | null {
    return this.loggedInUser;
  }

  logout() {
    this.authService.logout();
  }
}
