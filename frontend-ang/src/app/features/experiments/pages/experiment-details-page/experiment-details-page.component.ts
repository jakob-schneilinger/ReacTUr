import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../../../core/layout/layout.component';
import { ExperimentDetailsFormComponent } from '../../components/experiment-details-form/experiment-details-form.component';
import { FileUploadComponent } from '../../../../shared/file-upload/file-upload.component';
import { ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { ExperimentService } from '../../experiment.service';
import { GetExperimentDto, SaveExperimentDto } from '../../experiment.dto';
import { GetUserDto } from '../../../../shared/dtos/user.dto';
import { TranslocoService } from '@jsverse/transloco';
import ExperimentFileService from '../../experiment-file.service';
import { ExtractedMetadataConfirmModalComponent } from '../../components/extracted-metadata-confirm-modal/extracted-metadata-confirm-modal.component';
import { ExperimentHistoryDto } from '../../experiment-history.dto';
import { ExperimentHistoryModalComponent } from '../../components/experiment-history-modal/experiment-history-modal.component';
import { firstValueFrom, Observable } from 'rxjs';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';

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
    ExperimentHistoryModalComponent,
    TRANSLOCO_IMPORTS
  ],
})
export class ExperimentDetailsPageComponent implements OnInit {
  @ViewChild(ExperimentDetailsFormComponent) formComponent!: ExperimentDetailsFormComponent;

  state: FormState = 'create';
  user: GetUserDto | null = null;
  role: string = 'USER';

  experiment: GetExperimentDto | undefined;

  isConfirmMetadataModalOpen = false;
  pendingMetadata: any = null;
  pendingFileName: string = '';

  isHistoryModalOpen = false;
  experimentAuditLogs: ExperimentHistoryDto[] = [];
  isLoadingHistory = false;

  fakeFiles: File[] = [];
  pendingFiles: File[] = [];
  filesDirty: boolean = false;

  private uploadedFilesMap = new Map<string, string>();
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
    if (this.state === 'create') return this.transloco.translate('experiments.details.createNew');
    if (this.state === 'edit') return this.transloco.translate('experiments.details.editExisting');
    return this.transloco.translate('experiments.details.viewExisting');
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
          if (!!value.archivedAt) {
            this.notification.error(
              this.transloco.translate('experiments.details.notifications.loadError'),
              this.transloco.translate('experiments.details.notifications.loadArchivedError'),
            );
            this.router.navigate(['/search']);
            return;
          }
          this.experiment = value;
          this.loadFakeFiles();
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.notification.error(
            this.transloco.translate('experiments.details.notifications.loadError'),
            this.transloco.translate('experiments.details.notifications.loadExpError', {
              error: err.error || err.message || err,
            }),
          );
          this.router.navigate(['/search']);
          return;
        },
      });
    } else {
      this.state = 'create';
    }
  }

  private loadFakeFiles() {
    if (!this.experiment || !this.experiment.attachments) return;
    if (this.experiment.attachments.length === 0) return;

    let files: File[] = [];
    for (const attachment of this.experiment.attachments) {
      const fakeFile = new File([''], attachment.fileName, { type: `application/${attachment.fileType}` });

      (fakeFile as any).isFake = true;
      (fakeFile as any).urlId = attachment.uuid;

      files.push(fakeFile);

      this.uploadedFilesMap.set(this.getFileFingerprint(fakeFile), attachment.uuid);
    }

    this.fakeFiles = files;
    this.onFilesUploaded(this.fakeFiles)
  }

  onSaveExperiment(savePayload: SaveExperimentDto) {
    const request$: Observable<GetExperimentDto> =
      this.state === 'create'
        ? this.experimentService.createExperiment(savePayload)
        : this.experimentService.updateExperiment(this.experiment!.uuid, savePayload);

    request$.subscribe({
      next: (savedExperiment: GetExperimentDto) => {
        this.notification.success(
          this.transloco.translate('experiments.details.notifications.savedTitle'),
          this.transloco.translate('experiments.details.notifications.savedMessage'),
        );

        if (this.state === 'create' && this.pendingFiles.length > 0) {
          for (const file of this.pendingFiles) {
            this.experimentFileService.uploadFile(savedExperiment.uuid, file).subscribe({
              next: (response) => {
                console.log(`File uploaded successfully: ${file.name}`);
                this.router.navigate(['/experiments', savedExperiment.uuid]);
              },
              error: (err: any) => {
                console.log(err);
                this.router.navigate(['/experiments', savedExperiment.uuid]);
              },
            });
          }
        } else {
          this.router.navigate(['/experiments', savedExperiment.uuid]);
        }

        if (this.formComponent) {
          this.formComponent.experimentForm.markAsPristine();
        }
      },
      error: (err: any) => {
        this.notification.error(
          this.transloco.translate('experiments.details.notifications.savedErrorTitle'),
          this.transloco.translate('experiments.details.notifications.savedErrorMessage'),
        );
      },
    });
  }

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
      error: (err: any) => {
        console.log(err);
        this.notification.error(
          this.transloco.translate('experiments.details.notifications.errorTitle'),
          this.transloco.translate('experiments.details.notifications.historyLoadError'),
        );
        this.isLoadingHistory = false;
        this.cdr.markForCheck();
      },
    });
  }

  async onFilesUploaded(files: File[]): Promise<void> {
    if (this.state === 'create') {
      this.filesDirty = true;
      this.pendingFiles = files;
    } else {
      this.filesDirty = false;
      const currentFingerprints = new Set(files.map((f) => this.getFileFingerprint(f)));

      await this.handleDeletedFiles(currentFingerprints);

      const newFiles = files.filter(
        (file) => !this.uploadedFilesMap.has(this.getFileFingerprint(file)),
      );
      if (newFiles.length > 0) {
        await this.uploadDirectly(newFiles);
      }
    }

    await this.handleExtract(files);
  }

  private async handleDeletedFiles(currentFingerprints: Set<string>): Promise<void> {
    if (!this.experiment) return;

    for (const [fingerprint, fileIdentifier] of this.uploadedFilesMap.entries()) {
      if (!currentFingerprints.has(fingerprint)) {
        try {
          await firstValueFrom(this.experimentFileService.archiveFile(fileIdentifier));
          this.uploadedFilesMap.delete(fingerprint);
        } catch (error: any) {
          console.error(`Failed to delete file ${fileIdentifier}:`, error);
          this.notification.error(
            this.transloco.translate('experiments.details.notifications.deleteFileErrorTitle'),
            this.transloco.translate('experiments.details.notifications.deleteFileErrorMessage'),
          );
        }
      }
    }
  }

  private async uploadDirectly(newFiles: File[]): Promise<void> {
    if (!this.experiment) return;

    for (const file of newFiles) {
      if (!!(file as any).isFake) continue;
      try {
        const response = await firstValueFrom(
          this.experimentFileService.uploadFile(this.experiment.uuid, file),
        );

        const fileId = response.uuid || file.name;

        this.uploadedFilesMap.set(this.getFileFingerprint(file), fileId);
      } catch (error: any) {
        console.error(`Failed to upload ${file.name}:`, error);

        this.notification.error(
          this.transloco.translate('experiments.details.notifications.uploadErrorTitle'),
          this.transloco.translate('experiments.details.notifications.uploadErrorMessage', {
            fileName: file.name,
          }),
        );
      }
    }
  }

  private async handleExtract(files: File[]) {
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

  private getFileFingerprint(file: File): string {
    return `${file.name}-${file.size}-${file.lastModified}`;
  }

  confirmMetadataUse(selectedMetadata: Record<string, any>): void {
    if (selectedMetadata && this.formComponent) {
      this.formComponent.patchExtractedMetadata(selectedMetadata);
      this.notification.success(
        this.transloco.translate('reactants.extractedMetadata.buttons.appliedTitle'),
        this.transloco.translate('reactants.extractedMetadata.buttons.appliedMessage'),
      );
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
