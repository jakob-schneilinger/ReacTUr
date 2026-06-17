import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';
import { ExperimentService } from '../../../services/experiment.service';
import { ExperimentDataDto } from '../../../dtos/experiment.dto';
import { UserDto } from '../../../dtos/user.dto';

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
    private experimentService: ExperimentService,
  ) {}

  state: FormState = 'create';
  user: UserDto | null = null;
  role: string = "USER";

  experiment: ExperimentData | undefined;


  get formState() {
    return this.state;
  }

  get experimentTitle() {
    if (this.state === 'create') {
      return "Create new Experiment"
    } else if (this.state === 'edit') {
      return "Edit Experiment"
    } else {
      return "View Experiment"
    }
  }

  ngOnInit(): void {
    const state = this.route.snapshot.data['state'];
    if (state !== 'create') {
      this.user = this.authService.getLoggedInUser();
      if (this.user) {
        if (this.user.role) {
          this.role = this.user.role;
        }
      }
      this.state = state;
      const idString = this.route.snapshot.paramMap.get('id');
      if (!idString) {
        this.notification.error(
          'Invalid Experiment URL',
          'Please navigate to experiments/create or experiments/:id with a valid id!',
        );
        this.router.navigate(['/search'], { skipLocationChange: true });
        return;
      }
      const id = parseInt(idString);
      console.log("Test")
      if (this.role != 'ADMIN') {
        console.log('Test1');
        if (this.user?.email) {
          console.log('Test2');
          // TODO: check role and permissions, switch to 'view' if not enough permission
          this.authService.hasExperimentPermission(id, this.user?.email).subscribe({
            next: (hasPermission) =>
              hasPermission
                ? console.log('User has permission to edit experiment')
                : console.warn('User does not have permission to edit experiment'),
            error: (err) =>
              this.notification.error(
                'Permission Error',
                `Permission Error for this Experiment! Error: ${err}`,
              ),
          });
        }
      }
      this.experimentService.getExperimentData(id).subscribe({
        next: value => this.experiment = value,
        error: err => this.notification.error("Invalid Id", `No experiment found for this id: ${id}! Error: ${err}`)
        }
      )
      //TODO: load experiment data from backend
    }
    else {
      this.state = 'create';
    }
  }
}
