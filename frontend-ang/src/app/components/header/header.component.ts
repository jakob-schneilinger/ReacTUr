import { Component, OnInit } from "@angular/core";
import { AuthService } from "../../services/auth.service";
import { ActivatedRoute, Router } from "@angular/router";

@Component({
  selector: "app-header",
  templateUrl: "./header.component.html",
  styleUrls: ["./header.component.scss"],
  standalone: false,
})
export class HeaderComponent implements OnInit {


  constructor(
    public router: Router,
    public authService: AuthService,
  ) {
  }

  ngOnInit() {
    if (!this.authService.isLoggedIn()) return;
    const user = this.authService.getLoggedInUser();
  }

  logout() {
    this.authService.logoutUser();
    this.router.navigate(["/login"]);
  }
}
