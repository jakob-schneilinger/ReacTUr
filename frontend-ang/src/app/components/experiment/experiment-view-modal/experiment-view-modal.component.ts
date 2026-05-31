import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { Reactant } from '../../../dtos/reactant';
import { ReactantService } from '../../../services/reactant.service';
import { NotificationService } from '../../../services/notification.service';
import { Router } from '@angular/router';
import { Experiment } from '../../../dtos/experiment';
import { ExperimentService } from '../../../services/experiment.service';

@Component({
  selector: 'app-experiment-view-modal',
  templateUrl: './experiment-view-modal.component.html',
  styleUrls: ['./experiment-view-modal.component.scss'],
  standalone: false,
})
export class ExperimentViewModalComponent implements OnInit {
  @Input() selectedExperiment: Experiment | undefined;
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

      this.experimentService.getFiles(this.selectedExperiment.id).subscribe({
        next: (files: File[]) => {
          setTimeout(() => {
            /*
            if (this.selectedExperiment) {
              this.selectedExperiment.files = files;
            }
            this.cd.markForCheck();

             */
          });
        },
        error: (err) => {
          setTimeout(() => {
            this.notification.error('File Error', 'Unable to fetch Experiment Files!');
          });
        },
      });
    }
  }

  editExperiment(id: number | undefined): void {
    if (id) this.router.navigate(['/experiments', id]);
  }

  get headerText():string {
    return '#' + this.selectedExperiment?.id + ' ' + this.selectedExperiment?.reactants[0].shortName + ' + ' + this.selectedExperiment?.reactants[1]?.shortName;
  }
}
