import { Component, OnInit } from "@angular/core";
import { ReactiveFormsModule, UntypedFormBuilder, UntypedFormGroup, Validators } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from "../../../services/auth.service";
import { NotificationService } from "../../../services/notification.service";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss', '../auth.scss'],
  standalone: false,
})
export class LoginComponent implements OnInit {
  loginForm: UntypedFormGroup;
  // After first submission attempt, form validation will start
  submitted = false;

  gotoAfterSuccess: string = '/';

  constructor(
    private formBuilder: UntypedFormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
  ) {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
    });
  }

  /**
   * Form validation will start after the method is called, additionally an AuthRequest will be sent
   */
  loginUser(event: Event) {
    event.preventDefault();

    this.submitted = true;
    if (this.loginForm.valid) {
      /*
      const loginDto: UserLoginDto = {
        username: this.loginForm.controls.username.value,
        password: this.loginForm.controls.password.value,
      };
      this.authenticateUser(loginDto);
       */
    } else {
      console.log('Invalid input');
    }
  }

  /**
   * Send authentication data to the authService. If the authentication was successfully, the user will be forwarded to the message page
   *
   * @param loginDto authentication data from the user login form
  authenticateUser(loginDto: UserLoginDto) {
    console.log('Try to authenticate user: ' + loginDto.username);
    this.authService.loginUser(loginDto).subscribe({
      next: () => {
        console.log('Successfully logged in user: ' + loginDto.username);
        this.router.navigate([this.gotoAfterSuccess]);
      },
      error: (error) => {
        console.log('Could not log in due to:');
        console.log(error);

        if (typeof error.error === 'object') {
          this.notification.warn('Authentication problems!', error.error.error);
        } else {
          this.notification.warn('Authentication problems!', error.error);
        }
      },
    });
  }
   */

  ngOnInit() {
    this.gotoAfterSuccess = this.route.snapshot.queryParams['returnUrl'] || '/';
  }
}
