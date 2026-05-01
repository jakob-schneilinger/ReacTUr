

import { AuthService } from "../../services/auth.service";
import { ActivatedRoute, Router } from "@angular/router";
import { Component, OnInit, ViewChild, ElementRef, OnDestroy, ComponentRef } from "@angular/core";

// for delete modal
declare var bootstrap: any;

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  standalone: false,
})
export class HomeComponent {
  constructor(
    public authService: AuthService,
    public router: Router,
    public route: ActivatedRoute,
  ) {}
}
