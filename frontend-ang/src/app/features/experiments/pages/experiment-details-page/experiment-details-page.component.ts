import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../../../core/layout/layout.component';
import { ExperimentDetailsFormComponent } from '../../components/experiment-details-form/experiment-details-form.component';
import { FileUploadComponent } from '../../../../shared/file-upload/file-upload.component';
import { ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { ExperimentService } from '../../experiment.service';
import { GetExperimentDto } from '../../experiment.dto';
import { GetUserDto } from '../../../../shared/dtos/user.dto';
import { TranslocoService } from '@jsverse/transloco';
import ExperimentFileService from '../../experiment-file.service';
import {
  ExtractedMetadataConfirmModalComponent
} from '../../components/extracted-metadata-confirm-modal/extracted-metadata-confirm-modal.component';
import { ExperimentHistoryDto } from '../../experiment-history.dto';
import {
  ExperimentHistoryModalComponent
} from '../../components/experiment-history-modal/experiment-history-modal.component';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-experiment-details-site',
  templateUrl: './experiment-details-page.component.html',
  styleUrls: ['./experiment-details-page.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    LayoutComponent,
    ExperimentDetailsFormComponent,
    FileUploadComponent,
    ExtractedMetadataConfirmModalComponent,
    ExperimentHistoryModalComponent
  ]
})
export class ExperimentDetailsPageComponent implements OnInit {
  @ViewChild(ExperimentDetailsFormComponent) formComponent!: ExperimentDetailsFormComponent;

  state: FormState = 'create';
  user: GetUserDto | null = null;
  role: string = 'USER';

  experiment: GetExperimentDto | undefined;

  isConfirmMetadataModalOpen = false;
  pendingMetadata: any = null;
  pendingFileName: string = "";

  private processedMetadataFiles = new Set<string>();

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
    private experimentService: ExperimentService,
    private experimentFileService: ExperimentFileService,
    private cdr: ChangeDetectorRef,
    private transloco: TranslocoService,
  ) {}

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
        this.router.navigate(['/search']);
        return;
      }


      this.experimentService.getExperiment(experimentUuid).subscribe({
        next: (value: GetExperimentDto) => {
          this.experiment = value;
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.notification.error(
            this.transloco.translate('experiments.details.notifications.errorTitle'),
            this.transloco.translate('experiments.details.notifications.loadExpError', {
              error: err.error || err.message || err,
            }),
          );
          this.router.navigate(['/search']);
          return;
        },
      });

      /*
      if (this.role != 'ADMIN') {
        if (this.user?.email) {
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
      */
      //TODO: load experiment data from backend
    } else {
      this.state = 'create';
    }
  }

  isHistoryModalOpen = false;
  experimentAuditLogs: ExperimentHistoryDto[] = [];
  isLoadingHistory = false;

  openHistoryModal(): void {
    if (this.state === 'create' || !this.experiment?.uuid) return;

    this.isHistoryModalOpen = true;

      this.isLoadingHistory = true;

    this.experimentService.getExperimentHistory(this.experiment.uuid).subscribe({
      next: (history) => {
        this.experimentAuditLogs = history;
        this.isLoadingHistory = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.log(err)
        this.notification.error(
          'Error',
          'Could not load experiment history'
        );
        this.isLoadingHistory = false;
        this.cdr.markForCheck();
      }
    });
  }

  async onFilesUploaded(files: File[]): Promise<void> {
    // TODO: handle experiment file upload (probably save everything and upload after user click on submit
    if (this.state !== 'create') return;

    const currentFileNames = new Set(files.map((f) => f.name));
    for (const processedName of this.processedMetadataFiles) {
      if (!currentFileNames.has(processedName)) {
        this.processedMetadataFiles.delete(processedName);
      }
    }

    let metadataSet = false;
    for (const file of files) {
      if (!file.name.toLowerCase().endsWith('.csv')) {
        continue;
      }

      if (this.processedMetadataFiles.has(file.name)) {
        continue;
      }

      this.processedMetadataFiles.add(file.name);
      if (!metadataSet) {
        const metadata = await this.experimentFileService.extractMetadataFromFile(file);

        if (metadata) {
          console.log('Metadata extracted from:', file.name, metadata);
          this.pendingMetadata = metadata;
          this.pendingFileName = file.name;
          this.isConfirmMetadataModalOpen = true;
          this.cdr.markForCheck();
          metadataSet = true;
        }
      }
    }
  }

  confirmMetadataUse(selectedMetadata: Record<string, any>): void {
    if (selectedMetadata && this.formComponent) {
      this.formComponent.patchExtractedMetadata(selectedMetadata);
    }

    this.isConfirmMetadataModalOpen = false;
    this.pendingMetadata = null;
    this.pendingFileName = '';
  }

  declineMetadataUse(): void {
    this.isConfirmMetadataModalOpen = false;
    this.pendingMetadata = null;
    this.pendingFileName = '';
  }
}
