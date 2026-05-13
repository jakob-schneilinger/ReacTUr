import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { map } from 'rxjs';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-experiment-details-site',
  templateUrl: './experiment-details-site.component.html',
  styleUrls: ['./experiment-details-site.component.scss'],
  standalone: false,
})
export class ExperimentDetailsSiteComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
  ) {}

  state: FormState = 'create';

  ngOnInit(): void {
    const state = this.route.snapshot.data['state'];

    if (state === 'edit') {
      this.state = 'edit';
      const id = this.route.snapshot.paramMap.get('id');
      if (!id) {
        this.notification.error("Invalid Experiment URL", "Please navigate to experiments/create or experiments/:id with a valid id!");
        this.router.navigate(['/search'], { skipLocationChange: true });
        return;
      }
      //TODO: load experiment data from backend
    }
    else {
      this.state = 'create';
    }
  }
}
