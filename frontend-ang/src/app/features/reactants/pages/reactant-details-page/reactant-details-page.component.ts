import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../../../core/layout/layout.component';
import { ReactantFormComponent } from '../../components/reactant-form/reactant-form.component';
import { ReactantStructureComponent } from '../../components/reactant-structure/reactant-structure.component';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { ReactantService } from '../../reactant.service';
import { ReactantDetailDto, SaveReactantDto, StructureDto } from '../../reactant.dto';
import { filter, map, Subject, switchMap, takeUntil } from 'rxjs';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { StructureFlag } from '../../../../shared/dtos/enums/structure-flag.enum';
import { FileService } from '../../../../shared/services/file.service';
import { ReactantStructureSelectModalComponent } from '../../components/reactant-structure-select-modal/reactant-structure-select-modal.component';
import { FileUploadComponent } from '../../../../shared/file-upload/file-upload.component';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-reactant-details-site',
  templateUrl: './reactant-details-page.component.html',
  styleUrls: ['./reactant-details-page.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    LayoutComponent,
    ReactantFormComponent,
    ReactantStructureComponent,
    FileUploadComponent,
    ReactantStructureSelectModalComponent,
    TRANSLOCO_IMPORTS,
  ],
})
export class ReactantDetailsPageComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  reactant: ReactantDetailDto | undefined;
  smiles: string = '';
  loadingStructures: boolean = true;
  state: FormState = 'create';
  structures?: StructureDto[] = [];
  structureFiles: File[] = [];

  currentFlag?: StructureFlag;

  isStructureSelectionModalOpen: boolean = false;

  protected readonly StructureFlag = StructureFlag;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
    private reactantService: ReactantService,
    private fileService: FileService,
    private cd: ChangeDetectorRef,
    private transloco: TranslocoService,
  ) {}

  ngOnInit() {
    this.state = this.route.snapshot.data['state'] || 'create';

    if (this.state === 'edit') {
      this.loadReactantData();
    } else {
      this.loadingStructures = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadReactantData() {
    this.route.params
      .pipe(
        map((params) => params['uuid']),
        filter((uuid) => !!uuid),
        switchMap((uuid) => this.reactantService.getReactantDetail(uuid)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (details) => {
          this.reactant = details;
          this.currentFlag = details.flag;
          this.structures = details.structures;
          this.structureFiles = [];

          details.structures?.forEach((s) => {
            const file = this.parseFile(s);
            if (file) {
              this.structureFiles.push(file);
            }
          });
          this.loadingStructures = false;
          this.cd.markForCheck();
        },
        error: (err) => {
          this.loadingStructures = false;
          this.notification.error(
            this.transloco.translate('reactants.details.notifications.errorTitle'),
            err.error || this.transloco.translate('reactants.details.notifications.loadError'),
          );
          this.cd.markForCheck();
        },
      });
  }

  get isEditMode(): boolean {
    return this.state === 'edit';
  }

  get isCreateMode(): boolean {
    return this.state === 'create';
  }

  get showUpload(): boolean {
    return !(
      this.currentFlag === StructureFlag.PENDING || this.currentFlag === StructureFlag.READY
    );
  }

  updateSmiles(smiles: string) {
    this.smiles = smiles;
    if (this.reactant) {
      this.reactant.smiles = smiles;
    }
  }

  saveReactantForm(formValue: SaveReactantDto) {
    if (!this.isEditMode) {
      this.reactantService.createReactant(formValue).subscribe({
        next: (reactant) => {
          this.notification.success(
            this.transloco.translate('reactants.details.notifications.savedTitle'),
            this.transloco.translate('reactants.details.notifications.savedMessage'),
          );
          this.router.navigate(['/reactants', reactant.uuid]);
        },
        error: (err) => {
          console.warn(err);
          this.notification.error(
            this.transloco.translate('reactants.details.notifications.saveErrorTitle'),
            this.transloco.translate('reactants.details.notifications.saveErrorMessage', {
              error: err.error || err.message || err,
            }),
          );
        },
      });
    } else {
      if (!this.reactant) return;
      this.reactantService.updateReactant(this.reactant?.uuid, formValue).subscribe({
        next: (reactant) => {
          this.notification.success(
            this.transloco.translate('reactants.details.notifications.savedTitle'),
            this.transloco.translate('reactants.details.notifications.savedMessage'),
          );
          this.router.navigate(['/reactants', reactant.uuid]);
        },
        error: (err) =>
          this.notification.error(
            this.transloco.translate('reactants.details.notifications.saveErrorTitle'),
            this.transloco.translate('reactants.details.notifications.saveErrorMessage', {
              error: err.error || err.message || err,
            }),
          ),
      });
    }
  }

  handleUploadedFiles(files: File[]) {
    this.structureFiles = files;
    if (files.length === 1) {
      this.currentFlag = StructureFlag.ACTIVE;
    }
  }

  parseFile(structure: StructureDto): File | null {
    if (structure.structure && structure.name) {
      const fileBits = [structure.structure];

      const options = {
        type: 'text/plain;charset=utf-8',
        lastModified: Date.now(),
      };

      return new File(fileBits, structure.name, options);
    }
    return null;
  }

  generateStructureFromSmiles() {
    if (!this.smiles || !this.reactant || !this.reactant.uuid) return;

    this.currentFlag = StructureFlag.PENDING;

    this.fileService.generateStructureFromApi(this.reactant.uuid, this.smiles).subscribe({
      next: (value) => {
        this.notification.success(
          this.transloco.translate('reactants.details.notifications.generateTitle'),
          this.transloco.translate('reactants.details.notifications.generateMessage'),
        );
      },
      error: (err) => {
        this.notification.error(
          this.transloco.translate('reactants.details.notifications.generateErrorTitle'),
          this.transloco.translate('reactants.details.notifications.generateErrorMessage', {
            error: err.error || err.message || err,
          }),
        );
      },
    });
  }

  refreshStatus() {
    if (!this.reactant || !this.reactant.uuid) return;

    this.reactantService.getReactantDetailMock(this.reactant.uuid).subscribe({
      next: (value) => {
        console.log(value)
        this.reactant = value;
        if (value.structures && value.structures.length > 0) {
          this.currentFlag = value.structures[0].flag;
          this.structures = value.structures;
          this.structureFiles = [];
          value.structures?.forEach((s) => {
            const file = this.parseFile(s);
            if (file) {
              this.structureFiles.push(file);
            }
          });
          this.loadingStructures = false;

          this.notification.success('Flag: ' + this.currentFlag);

          this.cd.markForCheck();
        }
      },
    });
  }

  openStructureSelectionModal() {
    this.isStructureSelectionModalOpen = true;
  }

  handleStructureSelection(selectedStructure: StructureDto | null) {
    this.isStructureSelectionModalOpen = false;

    if (selectedStructure) {
      this.currentFlag = StructureFlag.ACTIVE;
      this.structures = [selectedStructure];
      const file = this.parseFile(selectedStructure);
      if (file) {
        this.structureFiles = [file];
      } else {
        this.structureFiles = [];
      }

      // TODO: Make an API call here to inform the backend which structure was selected
      // and update the reactant flag to ACTIVE.
    } else {
      this.currentFlag = undefined;
      this.structures = [];
      this.structureFiles = [];

      // TODO: Make an API call here to tell the backend to delete the pending structures
      // and reset the flag.
    }
  }
}
