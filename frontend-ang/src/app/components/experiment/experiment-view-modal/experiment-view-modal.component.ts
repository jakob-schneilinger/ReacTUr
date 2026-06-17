import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { ReactantDto } from '../../../dtos/reactant.dto';
import { ReactantService } from '../../../services/reactant.service';
import { NotificationService } from '../../../services/notification.service';
import { Router } from '@angular/router';
import { ExperimentDto, GetExperimentDto } from '../../../dtos/experiment.dto';
import { ExperimentService } from '../../../services/experiment.service';

@Component({
  selector: 'app-experiment-view-modal',
  templateUrl: './experiment-view-modal.component.html',
  styleUrls: ['./experiment-view-modal.component.scss'],
  standalone: false,
})
export class ExperimentViewModalComponent implements OnInit {
  @Input() selectedExperiment: GetExperimentDto | undefined;
  @Input() hasWritePermission: boolean = false;
  @Output() close = new EventEmitter<void>();

  constructor(
    private router: Router,
    private experimentService: ExperimentService,
    private notification: NotificationService,
    private cd: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    if (this.selectedExperiment) {
      /*
      this.selectedExperiment.attachments
      this.experimentService.getFiles(this.selectedExperiment.id).subscribe({
        next: (files: File[]) => {
          setTimeout(() => {

            if (this.selectedExperiment) {
              this.selectedExperiment.files = files;
            }
            this.cd.markForCheck();


          });
        },
        error: (err: any) => {
          setTimeout(() => {
            this.notification.error(
              'File Error',
              `Unable to fetch Experiment Files! Error: ${err}`,
            );
          });
        },
      });
      */
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
}
