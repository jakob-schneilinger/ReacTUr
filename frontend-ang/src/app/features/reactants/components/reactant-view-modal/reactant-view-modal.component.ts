import { CommonModule } from '@angular/common';
import { AppModalComponent } from '../../../../shared/modal/app-modal.component';
import { ReactantStructureComponent } from '../reactant-structure/reactant-structure.component';
import { Component, Input, Output, OnInit, ChangeDetectorRef, EventEmitter } from '@angular/core';
import { ReactantDetailDto, ReactantDto, StructureDto } from '../../reactant.dto';
import { ReactantService } from '../../reactant.service';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { Router } from '@angular/router';
import { UploadedFileComponent } from '../../../../shared/file-upload/uploaded-file/uploaded-file.component';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { StructureFlag } from '../../structure-flag.enum';
import { StructureService } from '../../structure.service';

@Component({
  selector: 'app-reactant-view-modal',
  templateUrl: './reactant-view-modal.component.html',
  styleUrls: ['./reactant-view-modal.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    AppModalComponent,
    ReactantStructureComponent,
    UploadedFileComponent,
    TRANSLOCO_IMPORTS,
  ],
})
export class ReactantViewModalComponent implements OnInit {
  @Input() selectedReactant: ReactantDto | undefined;
  @Input() hasWritePermission: boolean = false;
  @Output() close = new EventEmitter<void>();

  loadingStructures: boolean = true;

  reactant: ReactantDetailDto | undefined;

  constructor(
    private router: Router,
    private reactantService: ReactantService,
    private notification: NotificationService,
    private cd: ChangeDetectorRef,
    private transloco: TranslocoService,
    protected structureService: StructureService
  ) {}

  ngOnInit() {
    if (this.selectedReactant) {
      this.loadingStructures = true;
      this.reactantService.getReactantDetail(this.selectedReactant.uuid).subscribe({
        next: (reactant: ReactantDetailDto) => {
          this.reactant = reactant;
          this.loadingStructures = false;
          this.cd.markForCheck();
        },
        error: (err) => {
          this.loadingStructures = false;
          this.notification.error(
            this.transloco.translate('reactants.viewModal.notifications.fetchErrorTitle'),
            this.transloco.translate('reactants.viewModal.notifications.fetchErrorMessage'),
          );
          this.cd.markForCheck();
        },
      });
    }
  }

  get additionalNames() {
    return this.reactant?.additionalShortNames?.length
      ? this.reactant.additionalShortNames.join('; ')
      : '-';
  }

  get references() {
    return this.reactant?.references?.length ? this.reactant.references.join('; ') : '-';
  }

  editReactant(uuid: string | undefined): void {
    if (uuid) this.router.navigate(['/reactants', uuid]);
  }

  get activeStructure(): StructureDto | undefined {
    return this.reactant?.structures?.find((s) => s.flag === StructureFlag.ACTIVE);
  }

  get pendingStructures(): StructureDto[] {
    return (
      this.reactant?.structures?.filter(
        (s) => s.flag === StructureFlag.PENDING || s.flag === StructureFlag.READY,
      ) || []
    );
  }

  get failedStructures(): StructureDto[] {
    return this.reactant?.structures?.filter((s) => s.flag === StructureFlag.FAILED) || [];
  }
}
