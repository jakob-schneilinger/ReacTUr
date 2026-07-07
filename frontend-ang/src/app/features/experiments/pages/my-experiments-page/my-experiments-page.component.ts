import { ExperimentService } from '../../experiment.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { GetExperimentDto } from '../../experiment.dto';
import { GetUserDto } from '../../../../shared/dtos/user.dto';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { SearchResultsComponent } from '../../../search/components/search-results/search-results.component';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { LayoutComponent } from '../../../../core/layout/layout.component';

type ViewMode = 'active' | 'archived';

@Component({
  selector: 'app-my-experiments-page',
  standalone: true,
  imports: [
    CommonModule,
    TranslocoModule,
    TRANSLOCO_IMPORTS,
    SearchResultsComponent,
  ],
  templateUrl: './my-experiments-page.component.html',
  styleUrls: ['./my-experiments-page.component.scss'],
})
export class MyExperimentsPageComponent implements OnInit {
  viewMode: ViewMode = 'active';
  experiments: GetExperimentDto[] = [];
  isLoading = false;
  currentUser: GetUserDto | null = null;

  leftHalf: GetExperimentDto[] = [];
  rightHalf: GetExperimentDto[] = [];

  constructor(
    private experimentService: ExperimentService,
    private authService: AuthService,
    private notification: NotificationService,
    private transloco: TranslocoService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.isLoading = true;

    this.authService.getLoggedInUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.loadExperiments();
      },
      error: (err) => {
        this.isLoading = false;
        this.notification.error(
          this.transloco.translate('experiments.details.notifications.errorTitle'),
          this.transloco.translate('experiments.my.notifications.userLoadError'),
        );
        this.cdr.markForCheck();
      },
    });
  }

  onListChanged(): void {
    this.loadExperiments();
  }

  private splitExperiments(): void {
    const halfIndex = Math.ceil(this.experiments.length / 2);
    this.leftHalf = this.experiments.slice(0, halfIndex);
    this.rightHalf = this.experiments.slice(halfIndex);
  }

  toggleViewMode(mode: ViewMode): void {
    if (this.viewMode === mode) return;
    this.viewMode = mode;
    this.loadExperiments();
  }

  private loadExperiments(): void {
    if (!this.currentUser) return;

    this.isLoading = true;
    this.experiments = [];
    this.cdr.markForCheck();

    if (this.viewMode === 'archived') {
      this.experimentService.getArchivedExperiments().subscribe({
        next: (value) => {
          this.experiments = value;
          this.splitExperiments();
          this.isLoading = false;
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.isLoading = false;
          this.notification.error(
            this.transloco.translate('experiments.my.notifications.errorTitle'),
            this.transloco.translate('experiments.my.notifications.loadError'),
          );
          this.cdr.markForCheck();
        },
      });
    } else {
      this.experimentService.getExperimentsByScientist(this.currentUser.id).subscribe({
        next: value => {
          this.experiments = value.content;
          this.splitExperiments();
          this.isLoading = false;
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.isLoading = false;
          this.notification.error(
            this.transloco.translate('experiments.my.notifications.errorTitle'),
            this.transloco.translate('experiments.my.notifications.loadError'),
          );
          this.cdr.markForCheck();
        },
      });
    }
  }
}
