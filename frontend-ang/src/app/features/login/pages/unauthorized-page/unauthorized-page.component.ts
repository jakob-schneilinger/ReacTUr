import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Component, OnInit } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { Globals } from "../../../../core/global/globals";
import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  selector: 'app-unauthorized-page',
  templateUrl: './unauthorized-page.component.html',
  styleUrls: ['./unauthorized-page.component.scss'],
  standalone: true,
  imports: [CommonModule],
})
export class UnauthorizedPageComponent {
  constructor(
    public authService: AuthService,
    public router: Router,
    public route: ActivatedRoute,
  ) {}

  checkStatus(): void {
    window.location.reload();
  }

  logout(): void {
    this.authService.logout();
  }
}
