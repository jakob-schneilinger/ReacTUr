import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';
import { map } from 'rxjs';

@Component({
  selector: 'app-experiment-create-site',
  templateUrl: './experiment-create-site.component.html',
  styleUrls: ['./experiment-create-site.component.scss'],
  standalone: false,
})
export class ExperimentCreateSiteComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      const r1 = params['reactant1'];
      const r2 = params['reactant2'];

      if (r1 || r2) {
        console.log('Pre-filling form with:', r1, r2);
        // TODO: Add reactant to form
      }
    });
  }
}
