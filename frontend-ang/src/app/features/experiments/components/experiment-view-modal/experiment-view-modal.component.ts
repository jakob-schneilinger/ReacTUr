import { CommonModule } from '@angular/common';
import { AppModalComponent } from '../../../../shared/modal/app-modal.component';
import { UploadedFileComponent } from '../../../../shared/file-upload/uploaded-file/uploaded-file.component';
import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  ChangeDetectorRef,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { Router, RouterLink } from '@angular/router';
import { ExperimentAttachmentDto, GetExperimentDto } from '../../experiment.dto';
import { ExperimentService } from '../../experiment.service';

@Component({
  selector: 'app-experiment-view-modal',
  templateUrl: './experiment-view-modal.component.html',
  styleUrls: ['./experiment-view-modal.component.scss'],
  standalone: true,
  imports: [CommonModule, AppModalComponent, UploadedFileComponent, RouterLink],
})
export class ExperimentViewModalComponent implements OnInit, OnChanges {
  @Input() selectedExperiment: GetExperimentDto | undefined;
  @Input() hasWritePermission: boolean = false;
  @Output() close = new EventEmitter<void>();

  modalFiles: File[] = [];

  constructor(
    private router: Router,
    private experimentService: ExperimentService,
    private notification: NotificationService,
    private cd: ChangeDetectorRef,
  ) {}

  ngOnInit() {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedExperiment'] && this.selectedExperiment) {
      this.generateFakeFiles();
    }
  }

  private generateFakeFiles(): void {
    this.modalFiles = [];

    if (!this.selectedExperiment?.attachments) return;

    for (const attachment of this.selectedExperiment.attachments) {
      const fakeFile = new File([''], attachment.fileName);

      (fakeFile as any).isFake = true;
      (fakeFile as any).urlId = attachment.uuid;

      this.modalFiles.push(fakeFile);
    }
  }

  editExperiment(uuid: string | undefined): void {
    if (uuid) this.router.navigate(['/experiments', uuid]);
  }

  get headerText(): string {
    return (
      this.selectedExperiment?.reactantA.shortName +
      ' + ' +
      this.selectedExperiment?.reactantB.shortName
    );
  }

  planExperiment(experiment: GetExperimentDto | undefined): void {
    if (experiment?.measurementData.rateConstant) {
      this.router.navigate(['/experiment-planning'], {
        queryParams: {
          k: experiment.measurementData.rateConstant,
          nameA: experiment.reactantA?.shortName || 'A',
          nameB: experiment.reactantB?.shortName || 'B',
        },
      });
      this.close.emit();
    }
  }

  get concentrationText(): string {
    if (
      this.selectedExperiment?.metadata.concentrationA &&
      this.selectedExperiment.metadata.concentrationB
    ) {
      return (
        this.selectedExperiment.metadata.concentrationA +
        ' mM, ' +
        this.selectedExperiment.metadata.concentrationB +
        ' mM'
      );
    }
    return '-- mM';
  }
}
