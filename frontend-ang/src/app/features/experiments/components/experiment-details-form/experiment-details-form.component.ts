import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { AppConfirmModalComponent } from '../../../../shared/modal/confirm-modal.component';
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { GetUserDto } from '../../../../shared/dtos/user.dto';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { GetExperimentDto, SaveExperimentDto } from '../../experiment.dto';
import { Subject } from 'rxjs';
import { OptionService } from '../../../../shared/services/option.service';
import { ReactantSelectorComponent } from '../../../search/components/reactant-selector/reactant-selector.component';
import { ReactantDto } from '../../../reactants/reactant.dto';
import { ReactantService } from '../../../reactants/reactant.service';
import {
  InstrumentDto,
  ModalityDto,
  SolventDto,
} from '../../../../shared/dtos/selection-option.dto';
import { UserService } from '../../../../shared/services/user.service';
import { OptionSelectorComponent } from '../../../../shared/option-selector/option-selector.component';
import { StructureFlag } from '../../../reactants/structure-flag.enum';
import { ExperimentService } from '../../experiment.service';
import { DynamicArrayInputComponent } from '../../../../shared/dynamic-array-input/dynamic-array-input.component';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-experiment-details-form',
  templateUrl: './experiment-details-form.component.html',
  styleUrls: ['./experiment-details-form.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppConfirmModalComponent,
    TRANSLOCO_IMPORTS,
    ReactantSelectorComponent,
    OptionSelectorComponent,
    DynamicArrayInputComponent,
  ],
})
export class ExperimentDetailsFormComponent implements OnInit, OnDestroy, OnChanges {
  private destroy$ = new Subject<void>();

  @Input() formState: FormState = 'create';
  @Input() experiment?: GetExperimentDto;

  @Output() saveExperiment = new EventEmitter<SaveExperimentDto>();

  availableReactants: ReactantDto[] = [];
  isReactantsLoaded: boolean = false;
  initValA: string[] = [];
  initValB: string[] = [];
  triggerClearA: number = 0;
  triggerClearB: number = 0;

  experimentForm!: FormGroup;

  knownSolvents: SolventDto[] = [];
  knownInstruments: InstrumentDto[] = [];
  knownModalities: ModalityDto[] = [];
  knownUsers: GetUserDto[] = [];

  knownSolventNames: string[] = [];
  knownInstrumentNames: string[] = [];
  knownModalityNames: string[] = [];
  knownUserNames: string[] = [];

  filteredSolventNames: string[] = [];
  filteredInstrumentNames: string[] = [];
  filteredModalityNames: string[] = [];
  filteredUserNames: string[] = [];

  userOptions: { id: string; name: string }[] = [];

  user: GetUserDto | null = null;

  isArchiveExperimentModalOpen = false;
  isCancelExperimentModalOpen = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private notification: NotificationService,
    private authService: AuthService,
    private optionService: OptionService,
    private userService: UserService,
    private reactantService: ReactantService,
    private experimentService: ExperimentService,
    private cdr: ChangeDetectorRef,
    private transloco: TranslocoService,
  ) {}

  get scientistControl(): FormControl {
    return this.experimentForm.get('scientist') as FormControl;
  }

  get solventControl(): FormControl {
    return this.experimentForm.get('solvent') as FormControl;
  }

  get instrumentControl(): FormControl {
    return this.experimentForm.get('instrument') as FormControl;
  }

  get modalityControl(): FormControl {
    return this.experimentForm.get('modality') as FormControl;
  }

  get dois(): FormArray {
    return this.experimentForm.get('dois') as FormArray;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['experiment'] && this.experiment && this.experimentForm) {
      this.patchFormWithExperiment(this.experiment);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnInit(): void {
    this.authService.getLoggedInUser().subscribe((value) => (this.user = value));
    this.initForm();
    this.setupReactionAutoFill();
    this.setupErrorValidation();
    this.loadUsers();
    this.loadOptions();
    this.loadReactants();
  }

  private loadUsers() {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        let displayUsers = users.filter((u) => u.role !== 'UNAUTHORIZED' && !u.deactivatedAt);

        if (this.experiment?.metadata?.scientist) {
          const currentScientistId = this.experiment.metadata.scientist.id;
          const isMissing = !displayUsers.some((u) => u.id === currentScientistId);

          if (isMissing) {
            displayUsers.push(this.experiment.metadata.scientist);
          }
        }

        this.knownUsers = displayUsers;
        this.knownUserNames = this.knownUsers.map((o) => o.firstName + ' ' + o.lastName);
        this.filteredUserNames = [...this.knownUserNames];

        this.userOptions = this.knownUsers.map((u) => ({
          id: u.id,
          name: `${u.firstName} ${u.lastName}`,
        }));

        const scientistControl = this.experimentForm.get('scientist');
        scientistControl?.setValidators([
          Validators.required,
          this.inListValidator(this.knownUserNames),
        ]);
        scientistControl?.updateValueAndValidity({ emitEvent: false });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.notification.error(
          this.transloco.translate('experiments.details.notifications.userErrorTitle'),
          this.transloco.translate('experiments.details.notifications.userLoadError', {
            error: err.message || err.error || err,
          }),
        );
      },
    });
  }

  private loadOptions(): void {
    // Fetch Solvents
    this.optionService.getSelectionOptions('solvent').subscribe({
      next: (opts) => {
        let displaySolvents = opts.filter((o) => !o.archivedAt);

        if (this.experiment?.metadata?.solvent) {
          const currentUuid = this.experiment.metadata.solvent.uuid;
          const isMissing = !displaySolvents.some((o) => o.uuid === currentUuid);

          if (isMissing) {
            displaySolvents.push(this.experiment.metadata.solvent);
          }
        }

        this.knownSolvents = displaySolvents;
        this.knownSolventNames = this.knownSolvents.map((o) => o.name);
        this.filteredSolventNames = [...this.knownSolventNames];
        const solventControl = this.experimentForm.get('solvent');
        solventControl?.setValidators([
          Validators.required,
          this.inListValidator(this.knownSolventNames),
        ]);
        solventControl?.updateValueAndValidity({ emitEvent: false });
        this.cdr.markForCheck();
      },
      error: (err) => this.handleOptionError('solvents', err),
    });

    // Fetch Instruments
    this.optionService.getSelectionOptions('instrument').subscribe({
      next: (opts) => {
        let displayInstruments = opts.filter((o) => !o.archivedAt);

        if (this.experiment?.metadata?.instrument) {
          const currentUuid = this.experiment.metadata.instrument.uuid;
          const isMissing = !displayInstruments.some((o) => o.uuid === currentUuid);

          if (isMissing) {
            displayInstruments.push(this.experiment.metadata.instrument);
          }
        }

        this.knownInstruments = displayInstruments;
        this.knownInstrumentNames = this.knownInstruments.map((o) => o.name);
        this.filteredInstrumentNames = [...this.knownInstrumentNames];
        const instrumentControl = this.experimentForm.get('instrument');
        instrumentControl?.setValidators([this.inListValidator(this.knownInstrumentNames)]);
        instrumentControl?.updateValueAndValidity({ emitEvent: false });
        this.cdr.markForCheck();
      },
      error: (err) => this.handleOptionError('instruments', err),
    });

    // Fetch Modalities
    this.optionService.getSelectionOptions('modality').subscribe({
      next: (opts) => {
        let displayModalities = opts.filter((o) => !o.archivedAt);

        if (this.experiment?.metadata?.modality) {
          const currentUuid = this.experiment.metadata.modality.uuid;
          const isMissing = !displayModalities.some((o) => o.uuid === currentUuid);

          if (isMissing) {
            displayModalities.push(this.experiment.metadata.modality);
          }
        }

        this.knownModalities = displayModalities;
        this.knownModalityNames = this.knownModalities.map((o) => o.name);
        this.filteredModalityNames = [...this.knownModalityNames];
        const modalityControl = this.experimentForm.get('modality');
        modalityControl?.setValidators([this.inListValidator(this.knownModalityNames)]);
        modalityControl?.updateValueAndValidity({ emitEvent: false });
        this.cdr.markForCheck();
      },
      error: (err) => this.handleOptionError('modalities', err),
    });
  }

  private handleOptionError(category: string, err: any): void {
    this.notification.error(
      this.transloco.translate('experiments.details.notifications.optionLoadErrorTitle') ||
        'Connection Error',
      this.transloco.translate('experiments.details.notifications.optionLoadErrorMessage', {
        category: category,
        error: err.error || err.message || err,
      }),
    );
  }

  private loadReactants(): void {
    this.reactantService.getAllReactants().subscribe({
      next: (value) => {
        this.availableReactants = value.filter((r) => r.flag === StructureFlag.ACTIVE);
        this.isReactantsLoaded = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isReactantsLoaded = true;
        this.notification.error(
          this.transloco.translate('experiments.details.notifications.reactantErrorTitle'),
          this.transloco.translate('experiments.details.notifications.reactantLoadError', {
            error: err.message || err.error || err,
          }),
        );
      },
    });
  }

  get fullName(): string {
    return `${this.user?.firstName} ${this.user?.lastName}`;
  }

  get currentDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  private formatDate(date: string) {
    if (date) {
      return date.split('T')[0];
    }
    return null;
  }

  private initForm(): void {
    const isEditMode = this.formState !== 'create';

    this.experimentForm = this.fb.group({
      // Reactants
      reactantA: ['', [Validators.required, this.reactantUniqueValidator('A')]],
      reactantB: ['', [Validators.required, this.reactantUniqueValidator('B')]],

      // Required Metadata
      date: [this.currentDate, Validators.required],
      scientist: [this.fullName, [Validators.required, this.inListValidator(this.knownUserNames)]],
      temperature: ['', Validators.required],
      solvent: ['', [Validators.required, this.inListValidator(this.knownSolventNames)]],
      concentrationA: ['', [Validators.required, Validators.min(0)]],
      concentrationB: ['', [Validators.required, Validators.min(0)]],

      rateConstant: ['', [Validators.required, Validators.min(0)]],
      error: [5, [Validators.required, Validators.min(0)]],
      isErrorPercentage: [true, Validators.required],

      // Optional Metadata
      pFO_rateConstant: ['', Validators.min(0)],
      instrument: ['', [this.inListValidator(this.knownInstrumentNames)]],
      modality: ['', [this.inListValidator(this.knownModalityNames)]],
      wavelength: ['', Validators.min(0)],
      dois: this.fb.array([this.fb.control('')]),
      comment: ['', Validators.maxLength(1000)],
    });

    if (this.experiment && isEditMode) {
      this.patchFormWithExperiment(this.experiment);
    }
  }

  private patchFormWithExperiment(exp: GetExperimentDto): void {
    this.experimentForm.patchValue({
      reactantA: exp.reactantA.uuid || exp.reactantA.shortName || '',
      reactantB: exp.reactantB.uuid || exp.reactantB.shortName || '',
      date: this.formatDate(exp.metadata?.date) || this.currentDate,
      scientist:
        `${exp.metadata?.scientist?.firstName} ${exp.metadata?.scientist?.lastName}` ||
        this.fullName,
      temperature: exp.metadata?.temperature,
      solvent: exp.metadata?.solvent?.name,
      concentrationA: exp.metadata?.concentrationA,
      concentrationB: exp.metadata?.concentrationB,
      instrument: exp.metadata?.instrument?.name,
      modality: exp.metadata?.modality?.name,
      wavelength: exp.metadata?.wavelength,
      comment: exp.metadata?.comment,
      rateConstant: exp.measurementData?.rateConstant,
      pFO_rateConstant: exp.measurementData?.pFO_rateConstant,
      error: exp.measurementData?.error,
      isErrorPercentage: exp.measurementData?.isErrorPercentage,
    });

    this.syncDois(exp.dois);

    if (exp.reactantA) {
      this.initValA = [exp.reactantA.uuid || exp.reactantA.shortName];
    }
    if (exp.reactantB) {
      this.initValB = [exp.reactantB.uuid || exp.reactantB.shortName];
    }
  }

  private syncDois(dois?: string[]): void {
    this.dois.clear();
    if (dois && dois.length > 0) {
      dois.forEach((doi) => this.dois.push(this.fb.control(doi)));
    } else {
      this.dois.push(this.fb.control(''));
    }
  }

  public patchExtractedMetadata(metadata: any): void {
    if (metadata.date) {
      // yyyy/mm/dd
      if (metadata.date.includes('/')) {
        metadata.date = metadata.date.replace(/\//g, '-');
      }
      // dd.mm.yyyy
      else if (metadata.date.includes('.')) {
        const parts = metadata.date.split('.');
        if (parts.length === 3) {
          metadata.date = `${parts[2]}-${parts[1]}-${parts[0]}`;
        }
      }
    }

    if (metadata.reactantA) {
      this.initValA = [metadata.reactantA];
      delete metadata.reactantA;
    }
    if (metadata.reactantB) {
      this.initValB = [metadata.reactantB];
      delete metadata.reactantB;
    }

    this.experimentForm.patchValue(metadata);

    this.experimentForm.markAsDirty();
    this.experimentForm.updateValueAndValidity();

    this.notification.success(
      this.transloco.translate('experiments.details.notifications.metaDataTitle'),
      this.transloco.translate('experiments.details.notifications.metaDataMessage')
    );
  }

  private reactantUniqueValidator(identifier: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      const otherReactantValue = identifier === "A" ? this.experimentForm?.get('reactantB')?.value : this.experimentForm?.get('reactantA')?.value;
      const isUnique = value !== otherReactantValue;
      return isUnique ? null : { notUnique: { value: control.value } };
    };

  }

  private inListValidator(validOptions: string[]): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;

      const isValid = validOptions.includes(control.value);

      return isValid ? null : { notInList: { value: control.value } };
    };
  }

  private setupErrorValidation(): void {
    this.experimentForm
      .get('isErrorPercentage')
      ?.valueChanges.subscribe((isPercentage: boolean) => {
        const errorControl = this.experimentForm.get('error');

        if (!errorControl) return;

        if (isPercentage) {
          errorControl.setValidators([Validators.required, Validators.min(0), Validators.max(100)]);
        } else {
          errorControl.setValidators([Validators.required, Validators.min(0)]);
        }

        errorControl.updateValueAndValidity({ emitEvent: false });
      });
  }

  onReactantChange(type: 'A' | 'B', uuids: string[]): void {
    const val = uuids.length > 0 ? uuids[0] : '';
    const controlName = type === 'A' ? 'reactantA' : 'reactantB';
    const control = this.experimentForm.get(controlName);

    if (control) {
      control.setValue(val);
      control.markAsTouched();
      control.markAsDirty();
    }
  }

  private setupReactionAutoFill(): void {
    this.experimentForm.get('reactants')?.valueChanges.subscribe((reactants: string[]) => {
      const validReactants = reactants.filter((r) => r && r.trim() !== '');
      const reactionName = validReactants.join(' + ');
      this.experimentForm.patchValue({ reaction: reactionName }, { emitEvent: false });
    });
  }

  clearControl(controlName: string): void {
    const control = this.experimentForm.get(controlName);
    if (control) {
      control.setValue('');
      control.markAsTouched();
      control.markAsDirty();
    }
    if (controlName === 'reactantA') {
      this.triggerClearA++;
    } else if (controlName === 'reactantB') {
      this.triggerClearB++;
    }
  }

  isInvalid(controlName: string): boolean {
    const control = this.experimentForm.get(controlName);
    return !!(control && control.invalid && control.touched);
  }

  onSubmit(): void {
    if (this.experimentForm.valid) {
      const formValue = this.experimentForm.getRawValue();

      formValue.scientist = this.knownUsers.find(
        (u) =>
          formValue.scientist.includes(u.lastName + ' ' + u.firstName) ||
          formValue.scientist.includes(u.firstName + ' ' + u.lastName),
      )?.id;
      formValue.solvent = this.knownSolvents.find((s) => s.name === formValue.solvent)?.uuid;
      formValue.instrument = this.knownInstruments.find(
        (i) => i.name === formValue.instrument,
      )?.uuid;
      formValue.modality = this.knownModalities.find((m) => m.name === formValue.modality)?.uuid;
      formValue.dois = formValue.dois.filter((val: string) => val && val.trim() !== '');

      const savePayload: SaveExperimentDto = {
        reactantA: formValue.reactantA,
        reactantB: formValue.reactantB,
        metadata: {
          date: formValue.date,
          scientist: formValue.scientist,
          temperature: formValue.temperature,
          solvent: formValue.solvent,
          concentrationA: formValue.concentrationA,
          concentrationB: formValue.concentrationB,
          instrument: formValue.instrument || undefined,
          modality: formValue.modality || undefined,
          wavelength: formValue.wavelength || undefined,
          comment: formValue.comment || undefined,
        },
        measurementData: {
          pFO_rateConstant: formValue.pFO_rateConstant || undefined,
          rateConstant: formValue.rateConstant,
          error: formValue.error,
          isErrorPercentage: formValue.isErrorPercentage,
        },
        dois: formValue.dois,
      };

      this.saveExperiment.emit(savePayload);
    } else {
      this.experimentForm.markAllAsTouched();
      this.notification.error(
        this.transloco.translate('experiments.details.notifications.savedErrorTitle'),
        this.transloco.translate('experiments.details.form.errors.formInvalid'),
      );
    }
  }

  // ####################
  // ## MODAL HANDLING ##
  // ####################

  onCancel(): void {
    if (this.experimentForm.dirty) {
      this.isCancelExperimentModalOpen = true;
    } else {
      this.executeCancel();
    }
  }

  executeCancel(): void {
    this.isCancelExperimentModalOpen = false;
    this.experimentForm.reset();
    this.router.navigate(['/search']);
  }

  onArchive(): void {
    this.isArchiveExperimentModalOpen = true;
  }

  executeArchive(): void {
    this.isArchiveExperimentModalOpen = false;
    if (!this.experiment) return;
    this.experimentService.archiveExperiment(this.experiment.uuid).subscribe({
      next: (value) => {
        this.notification.success(
          this.transloco.translate('experiments.details.notifications.archivedTitle'),
          this.transloco.translate('experiments.details.notifications.archivedMessage'),
        );
        this.experimentForm.reset();
        this.router.navigate(['/search']);
      },
      error: (err) => {
        this.notification.error(
          this.transloco.translate('experiments.details.notifications.archiveFailedTitle'),
          this.transloco.translate('experiments.details.notifications.archiveFailedMessage', {
            error: err.error || err.message || err,
          }),
        );
      },
    });
  }

  closeModals(): void {
    this.isCancelExperimentModalOpen = false;
    this.isArchiveExperimentModalOpen = false;
  }
}
