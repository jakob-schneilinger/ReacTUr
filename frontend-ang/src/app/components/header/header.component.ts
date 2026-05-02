import { Component, HostListener, OnInit, ElementRef } from '@angular/core';
import { AuthService } from "../../services/auth.service";
import { ActivatedRoute, Router } from "@angular/router";
import { User } from '../../dtos/user';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  standalone: false,
})
export class HeaderComponent implements OnInit {
  constructor(
    public router: Router,
    public authService: AuthService,
    private eRef: ElementRef,
  ) {}
  isDropdownOpen: boolean = false;
  user: User | null = null;

  ngOnInit() {
    if (!this.authService.isLoggedIn()) return;
    this.user = this.authService.getLoggedInUser();
    console.log(this.user);
  }

  toggleDropdown(event: Event) {
    event.stopPropagation();
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  @HostListener('document:click', ['$event'])
  clickout(event: Event) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.isDropdownOpen = false;
    }
  }

  logout() {
    this.authService.logoutUser();
    this.router.navigate(['/login']);
  }
}
