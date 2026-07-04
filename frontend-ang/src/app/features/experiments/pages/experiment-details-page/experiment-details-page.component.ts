import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../../../core/layout/layout.component';
import { ExperimentDetailsFormComponent } from '../../components/experiment-details-form/experiment-details-form.component';
import { FileUploadComponent } from '../../../../shared/file-upload/file-upload.component';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { ExperimentService } from '../../experiment.service';
import { GetExperimentDto } from '../../experiment.dto';
import { GetUserDto } from '../../../../shared/dtos/user.dto';
import { TranslocoService } from '@jsverse/transloco';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-experiment-details-site',
  templateUrl: './experiment-details-page.component.html',
  styleUrls: ['./experiment-details-page.component.scss'],
  standalone: true,
  imports: [CommonModule, LayoutComponent, ExperimentDetailsFormComponent, FileUploadComponent],
})
export class ExperimentDetailsPageComponent implements OnInit {
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
      this.authService.getLoggedInUser().subscribe({
        next: (value) => {
          this.user = value;
          if (this.user) {
            if (this.user.role) {
              this.role = this.user.role;
            }
          }
        },
      });
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
          this.authService.hasExperimentPermission(experimentUuid, this.user?.id).subscribe({
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
                    error: err.error || err.message || err,
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
              error: err.error || err.message || err,
            }),
          );
        }
      });
      //TODO: load experiment data from backend
    } else {
      this.state = 'create';
    }
  }
}
