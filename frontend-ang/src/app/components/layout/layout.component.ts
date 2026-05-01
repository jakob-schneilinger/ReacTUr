import { Component, OnInit } from "@angular/core";
import { AuthService } from "../../services/auth.service";
import { ActivatedRoute, Router } from "@angular/router";

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss'],
  standalone: false,
})
export class LayoutComponent {

  constructor(
    public router: Router,
    public authService: AuthService,
  ) {}
}
