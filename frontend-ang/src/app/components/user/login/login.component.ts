import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { AuthService } from "../../../services/auth.service";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss', '../auth.scss'],
  standalone: false,
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
