import { CommonModule } from '@angular/common';
import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { AuthService } from "../../core/auth/auth.service";
import { TRANSLOCO_IMPORTS } from '../../core/global/transloco-imports';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss', './auth.scss'],
  standalone: true,
  imports: [CommonModule, TRANSLOCO_IMPORTS],
})
export class LoginComponent implements OnInit {
  gotoAfterSuccess: string = '/';

  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.gotoAfterSuccess = this.route.snapshot.queryParams['returnUrl'] || '/';
  }

  // Wird vom Button im HTML aufgerufen
  loginUser() {
    this.authService.login();
  }
}
