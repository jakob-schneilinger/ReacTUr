import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { Reactant } from '../../../dtos/reactant';
import { ReactantService } from '../../../services/reactant.service';
import { NotificationService } from '../../../services/notification.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reactant-view-modal',
  templateUrl: './reactant-view-modal.component.html',
  styleUrls: ['./reactant-view-modal.component.scss'],
  standalone: false,
})
export class ReactantViewModalComponent implements OnInit {
  @Input() selectedReactant: Reactant | undefined;
  @Output() close = new EventEmitter<void>();

  loadingStructures: boolean = true;

  constructor(
    private router: Router,
    private experimentService: ReactantService,
    private notification: NotificationService,
    private cd: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    if (this.selectedReactant) {
      this.loadingStructures = true;

      this.experimentService.getReactantStructures(this.selectedReactant.shortName).subscribe({
        next: (files: File[]) => {
          setTimeout(() => {
            if (this.selectedReactant) {
              this.selectedReactant.structures = files;
            }
            this.loadingStructures = false;
            this.cd.markForCheck();
          });
        },
        error: (err) => {
          setTimeout(() => {
            this.loadingStructures = false;
            this.notification.error('Missing 3D File', '3D File(s) of Reactant not found!');
            this.cd.markForCheck();
          });
        },
      });
    }
  }

  editReactant(shortName: string | undefined): void {
    if (shortName) this.router.navigate(['/reactants', shortName]);
  }
}
