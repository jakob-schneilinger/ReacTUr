import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../../../core/layout/layout.component';
import { ReactantFormComponent } from '../../components/reactant-form/reactant-form.component';
import { ReactantStructureComponent } from '../../components/reactant-structure/reactant-structure.component';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { ReactantService } from '../../reactant.service';
import { ReactantDetailDto, SaveReactantDto, StructureDto } from '../../reactant.dto';
import { filter, map, Subject, switchMap, takeUntil, timer } from 'rxjs';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { StructureFlag } from '../../structure-flag.enum';
import { ReactantStructureSelectModalComponent } from '../../components/reactant-structure-select-modal/reactant-structure-select-modal.component';
import { FileUploadComponent } from '../../../../shared/file-upload/file-upload.component';
import { StructureService } from '../../structure.service';
import { ReactantStructureConfirmModalComponent } from '../../components/reactant-structure-confirm-modal/reactant-structure-confirm-modal.component';
import { UploadedFileComponent } from '../../../../shared/file-upload/uploaded-file/uploaded-file.component';

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
    ReactantStructureConfirmModalComponent,
    UploadedFileComponent,
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

  isRefreshDisabled: boolean = false;

  isStructureSelectionModalOpen: boolean = false;
  isStructureConfirmModalOpen: boolean = false;
  uploadedStructureFile: File | null = null;

  protected readonly StructureFlag = StructureFlag;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
    private reactantService: ReactantService,
    private structureService: StructureService,
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
          this.structures = details.structures;
          this.currentFlag = this.determineStateFlag(this.structures);
          this.structureFiles = [];

          details.structures?.forEach((s) => {
            const file = this.structureService.parseFile(s, details.shortName);
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

  private determineStateFlag(structures?: StructureDto[]): StructureFlag | undefined {
    if (!structures || structures.length === 0) return undefined;

    if (structures.some((s) => s.flag === StructureFlag.ACTIVE)) return StructureFlag.ACTIVE;
    if (structures.some((s) => s.flag === StructureFlag.READY)) return StructureFlag.READY;
    if (structures.some((s) => s.flag === StructureFlag.PENDING)) return StructureFlag.PENDING;
    if (structures.some((s) => s.flag === StructureFlag.FAILED)) return StructureFlag.FAILED;

    return undefined;
  }

  get isEditMode(): boolean {
    return this.state === 'edit';
  }

  get isCreateMode(): boolean {
    return this.state === 'create';
  }

  get showUpload(): boolean {
    return !(
      this.currentFlag === StructureFlag.PENDING || this.currentFlag === StructureFlag.READY || this.currentFlag === StructureFlag.ACTIVE
    );
  }

  get showUploadedFile(): boolean {
    return this.currentFlag === StructureFlag.ACTIVE
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
    if (files.length === 1) {
      this.uploadedStructureFile = files[0];
      this.isStructureConfirmModalOpen = true;
    }
  }

  cancelUploadedStructure() {
    this.isStructureConfirmModalOpen = false;
    this.uploadedStructureFile = null;
    this.structureFiles = [];
  }

  confirmUploadedStructure() {
    this.isStructureConfirmModalOpen = false;
    if (!this.uploadedStructureFile) return;
    this.structureFiles = [this.uploadedStructureFile];
    this.uploadedStructureFile = null;
    if (this.reactant) {
      this.structureService.uploadStructure(this.reactant.uuid, this.structureFiles[0]).subscribe({
        next: (value) => {
          this.notification.success(
            this.transloco.translate('reactants.details.notifications.uploadTitle'),
            this.transloco.translate('reactants.details.notifications.uploadMessage'),
          );
          this.refreshStatus();
        },
        error: (err) => {
          this.notification.error(
            this.transloco.translate('reactants.details.notifications.uploadErrorTitle'),
            this.transloco.translate('reactants.details.notifications.uploadErrorMessage', {error: err.error || err.message || err}),
          );
        },
      });
    }
  }

  generateStructureFromSmiles() {
    if (!this.smiles || !this.reactant || !this.reactant.uuid) return;

    this.currentFlag = StructureFlag.PENDING;

    this.structureService.generateStructureFromApi(this.reactant.uuid).subscribe({
      next: (value) => {
        this.notification.success(
          this.transloco.translate('reactants.details.notifications.generateTitle')
        );
        this.refreshStatus();
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
    if (!this.reactant || !this.reactant.uuid || this.isRefreshDisabled) return;

    this.isRefreshDisabled = true;
    this.cd.markForCheck();

    timer(5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.isRefreshDisabled = false;
        this.cd.markForCheck();
      });

    this.reactantService.getReactantDetail(this.reactant.uuid).subscribe({
      next: (value) => {
        this.reactant = value;
        if (value.structures && value.structures.length > 0) {
          this.currentFlag = this.determineStateFlag(value.structures);
          this.structures = value.structures;
          this.structureFiles = [];
          value.structures?.forEach((s) => {
            const file = this.structureService.parseFile(s, value.shortName);
            if (file) {
              this.structureFiles.push(file);
            }
          });
        }
        this.loadingStructures = false;
        this.notification.success(
          this.transloco.translate('reactants.details.notifications.pageRefreshTitle'), "",
          1000
        );
        this.cd.markForCheck();
      },
    });
  }

  openStructureSelectionModal() {
    this.isStructureSelectionModalOpen = true;
  }

  handleStructureSelection(selectedStructure: StructureDto | null) {
    this.isStructureSelectionModalOpen = false;

    //TODO: overthink structure rejection. if reject selectedStructure = null

    if (selectedStructure && selectedStructure.id) {
      this.structureService.selectStructure(selectedStructure.id).subscribe({
        next: () => {
          this.notification.success(
            this.transloco.translate('reactants.details.notifications.structureSelectTitle')
          );
          this.refreshStatus();
        },
        error: (err) => {
          this.notification.error(
            this.transloco.translate('reactants.details.notifications.structureSelectErrorTitle'),
            this.transloco.translate('reactants.details.notifications.structureSelectErrorMessage'),
          );
          this.refreshStatus();
        },
      });
    } else {
      this.refreshStatus();
    }
  }

  get readyStructures(): StructureDto[] {
    return this.structures?.filter((s) => s.flag === StructureFlag.READY) || [];
  }
}
