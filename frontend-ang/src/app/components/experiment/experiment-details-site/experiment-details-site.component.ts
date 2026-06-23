import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { ExperimentDetailsFormComponent } from './experiment-detail-form/experiment-details-form.component';
import { FileUploadComponent } from '../../shared/file-upload/file-upload.component';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';
import { ExperimentService } from '../../../services/experiment.service';
import { GetExperimentDto } from '../../../dtos/experiment.dto';
import { GetUserDto } from '../../../dtos/user.dto';
import { TranslocoService } from '@jsverse/transloco';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-experiment-details-site',
  templateUrl: './experiment-details-site.component.html',
  styleUrls: ['./experiment-details-site.component.scss'],
  standalone: true,
  imports: [CommonModule, LayoutComponent, ExperimentDetailsFormComponent, FileUploadComponent],
})
export class ExperimentDetailsSiteComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
    private experimentService: ExperimentService,
    private cdr: ChangeDetectorRef,
    private transloco: TranslocoService,
  ) {}

  state: FormState = 'create';
  user: GetUserDto | null = null;
  role: string = 'USER';

  experiment: GetExperimentDto | undefined;

  get formState() {
    return this.state;
  }

  get experimentTitle() {
    if (this.state === 'create') {
      return this.transloco.translate('experiments.details.createNew');
    } else if (this.state === 'edit') {
      return this.transloco.translate('experiments.details.editExisting');
    } else {
      return this.transloco.translate('experiments.details.viewExisting');
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
      const experimentUuid = this.route.snapshot.paramMap.get('uuid');
      if (!experimentUuid) {
        this.notification.error(
          this.transloco.translate('experiments.details.notifications.errorTitle'),
          this.transloco.translate('experiments.details.notifications.loadError'),
        );
        this.router.navigate(['/search'], { skipLocationChange: true });
        return;
      }
      if (this.role != 'ADMIN') {
        console.log('Test1');
        if (this.user?.email) {
          console.log('Test2');
          // TODO: check role and permissions, switch to 'view' if not enough permission
          this.authService.hasExperimentPermission(experimentUuid, this.user?.uuid).subscribe({
            next: (hasPermission) =>
              hasPermission
                ? console.log('User has permission to edit experiment')
                : console.warn('User does not have permission to edit experiment'),
            error: (err) =>
              this.notification.error(
                this.transloco.translate('experiments.details.notifications.permissionErrorTitle'),
                this.transloco.translate(
                  'experiments.details.notifications.permissionErrorMessage',
                  {
                    error: err.message || err.error || err,
                  },
                ),
              ),
          });
        }
      }
      this.experimentService.getExperiment(experimentUuid).subscribe({
        next: (value: GetExperimentDto) => {
          this.experiment = value
          this.cdr.markForCheck()
        },
        error: (err: any) => {
          this.notification.error(
            this.transloco.translate('experiments.details.notifications.errorTitle'),
            this.transloco.translate('experiments.details.notifications.loadExpError', {
              error: err.message || err.error || err,
            }),
          )
        }
      });
      //TODO: load experiment data from backend
    } else {
      this.state = 'create';
    }
  }
}
