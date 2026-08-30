import { CommonModule, NgOptimizedImage } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AuthService } from "../../auth/auth.service";
import { Router, RouterModule } from '@angular/router';
import { GetUserDto } from '../../../shared/dtos/user.dto';
import { HasRoleDirective } from '../../../shared/directives/has-role.directive';
import { NgbDropdown, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle } from '@ng-bootstrap/ng-bootstrap';
import { TRANSLOCO_IMPORTS } from '../../global/transloco-imports';
import { AvailableLangs, LangDefinition, TranslocoService } from '@jsverse/transloco';

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
    private transloco: TranslocoService,
    private cdr: ChangeDetectorRef,
  ) {}

  loggedInUser: GetUserDto | null = null;

  showTooltip: boolean = true;

  ngOnInit(): void {
    this.authService.initialLoginSequence.then(() => {
      this.authService.getLoggedInUser().subscribe((user) => {
        this.loggedInUser = user;
        this.cdr.markForCheck();
      });
    });
  }

  toggleTooltips() {
    this.showTooltip = !this.showTooltip;
  }

  getLangId(lang: any): string {
    return typeof lang === 'string' ? lang : lang.id;
  }

  getLangLabel(lang: any): string {
    return typeof lang === 'string' ? lang : lang.label;
  }

  get activeLanguage(): string {
    return this.transloco.getActiveLang();
  }

  setActiveLanguage(lang: string) {
    this.transloco.setActiveLang(lang);
  }

  get availableLanguages(): AvailableLangs {
    return this.transloco.getAvailableLangs();
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
