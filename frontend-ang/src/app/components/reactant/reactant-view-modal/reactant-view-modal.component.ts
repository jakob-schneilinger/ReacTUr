import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { ReactantDetailDto, ReactantDto } from '../../../dtos/reactant.dto';
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
  @Input() selectedReactant: ReactantDetailDto | undefined;
  @Input() hasWritePermission: boolean = false;
  @Output() close = new EventEmitter<void>();

  loadingStructures: boolean = true;

  constructor(
    private router: Router,
    private reactantService: ReactantService,
    private notification: NotificationService,
    private cd: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    if (this.selectedReactant) {
      this.loadingStructures = true;

      this.reactantService.getReactantStructures(this.selectedReactant).subscribe({
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

  get additionalNames() {
    return this.selectedReactant?.additionalShortNames?.length
      ? this.selectedReactant.additionalShortNames.join('; ')
      : '-';
  }

  get references() {
    return this.selectedReactant?.references?.length
      ? this.selectedReactant.references.join('; ')
      : '-';
  }

  editReactant(uuid: string | undefined): void {
    if (uuid) this.router.navigate(['/reactants', uuid]);
  }
}
