import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { AppConfirmModalComponent } from '../../../shared/modal/confirm-modal.component';
import {
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
} from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, ValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { NotificationService } from '../../../../services/notification.service';
import { AuthService } from '../../../../services/auth.service';
import { GetUserDto } from '../../../../dtos/user.dto';
import { TRANSLOCO_IMPORTS } from '../../../../global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { ExperimentService } from '../../../../services/experiment.service';
import { GetExperimentDto, SaveExperimentDto } from '../../../../dtos/experiment.dto';
import { Observable, Subject } from 'rxjs';
import { OptionService } from '../../../../services/option.service';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-experiment-details-form',
  templateUrl: './experiment-details-form.component.html',
  styleUrls: ['./experiment-details-form.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppConfirmModalComponent, TRANSLOCO_IMPORTS],
})
export class ExperimentDetailsFormComponent implements OnInit, OnDestroy, OnChanges {
  private destroy$ = new Subject<void>();
  @Input() formState: FormState = 'create';
  @Input() experiment?: GetExperimentDto;

  isArchiveExperimentModalOpen = false;
  isCancelExperimentModalOpen = false;

  experimentForm!: FormGroup;

  knownSolvents: string[] = [];
  knownInstruments: string[] = [];
  knownModalities: string[] = [];

  filteredSolvents: string[] = [];
  filteredInstruments: string[] = [];
  filteredModalities: string[] = [];

  user: GetUserDto | null = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private notification: NotificationService,
    private authService: AuthService,
    private optionService: OptionService,
    private experimentService: ExperimentService,
    private cdr: ChangeDetectorRef,
    private transloco: TranslocoService,
  ) {}

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
    this.user = this.authService.getLoggedInUser();
    this.initForm();
    this.setupReactionAutoFill();
    this.loadOptions();
  }

  private loadOptions(): void {
    // Fetch Solvents
    this.optionService.getSelectionOptions('solvent').subscribe({
      next: (opts) => {
        this.knownSolvents = opts.map((o) => o.name);
        this.filteredSolvents = [...this.knownSolvents];
        const solventControl = this.experimentForm.get('solvent');
        solventControl?.setValidators([
          Validators.required,
          this.inListValidator(this.knownSolvents),
        ]);
        solventControl?.updateValueAndValidity({ emitEvent: false });
        this.cdr.markForCheck();
      },
      error: (err) => this.handleOptionError('solvents', err),
    });

    // Fetch Instruments
    this.optionService.getSelectionOptions('instrument').subscribe({
      next: (opts) => {
        this.knownInstruments = opts.map((o) => o.name);
        this.filteredInstruments = [...this.knownInstruments];
        const instrumentControl = this.experimentForm.get('instrument');
        instrumentControl?.setValidators([this.inListValidator(this.knownInstruments)]);
        instrumentControl?.updateValueAndValidity({ emitEvent: false });
        this.cdr.markForCheck();
      },
      error: (err) => this.handleOptionError('instruments', err),
    });

    // Fetch Modalities
    this.optionService.getSelectionOptions('modality').subscribe({
      next: (opts) => {
        this.knownModalities = opts.map((o) => o.name);
        this.filteredModalities = [...this.knownModalities];
        const modalityControl = this.experimentForm.get('modality');
        modalityControl?.setValidators([this.inListValidator(this.knownModalities)]);
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
        error: err.message || err.error || err,
      }),
    );
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
  };

  private initForm(): void {
    const isEditMode = this.formState !== 'create';

    this.experimentForm = this.fb.group({
      // Reactants
      reactantA: [{ value: '', disabled: isEditMode }, Validators.required],
      reactantB: [{ value: '', disabled: isEditMode }, Validators.required],

      // Required Metadata
      date: [this.currentDate, Validators.required],
      scientist: [this.fullName, Validators.required],
      temperature: ['', Validators.required],
      solvent: ['', [Validators.required, this.inListValidator(this.knownSolvents)]],
      concentrationA: ['', [Validators.required, Validators.min(0)]],
      concentrationB: ['', [Validators.required, Validators.min(0)]],

      // Optional Metadata
      instrument: ['', [this.inListValidator(this.knownInstruments)]],
      modality: ['', [this.inListValidator(this.knownModalities)]],
      wavelength: ['', Validators.min(0)],
      comment: [''],
    });

    //TODO: if edit mode init form with existing data
    if (this.experiment) {
      this.patchFormWithExperiment(this.experiment);
    }
  }

  private patchFormWithExperiment(exp: GetExperimentDto): void {
    this.experimentForm.patchValue({
      reactantA: exp.reactantA.shortName || '',
      reactantB: exp.reactantB.shortName || '',
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
    });
  }

  private inListValidator(validOptions: string[]): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;

      const isValid = validOptions.includes(control.value);

      return isValid ? null : { notInList: { value: control.value } };
    };
  }

  get reactantsControls() {
    return (this.experimentForm.get('reactants') as FormArray).controls;
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
  }

  isInvalid(controlName: string): boolean {
    const control = this.experimentForm.get(controlName);
    return !!(control && control.invalid && control.touched);
  }

  isReactantInvalid(index: number): boolean {
    const control = this.reactantsControls[index];
    return !!(control && control.invalid && control.touched);
  }

  onSubmit(): void {
    // TODO: send to backend and wait for success
    if (this.experimentForm.valid) {
      const formValue = this.experimentForm.getRawValue();

      const saveExperiment: SaveExperimentDto = {
        ...formValue,
      };

      const request$: Observable<GetExperimentDto> =
        this.formState === 'create'
          ? this.experimentService.createExperiment(saveExperiment)
          : this.experimentService.patchExperiment(this.experiment!.uuid, saveExperiment);

      request$.subscribe({
        next: (value) => {
          this.experimentForm.markAsPristine();
          this.notification.success(
            this.transloco.translate('experiments.details.notifications.savedTitle'),
            this.transloco.translate('experiments.details.notifications.savedMessage'),
          );
        },
        error: (err) => {
          this.notification.error(
            this.transloco.translate('experiments.details.notifications.savedErrorTitle'),
            this.transloco.translate('experiments.details.notifications.savedErrorMessage'),
          );
        },
      });
    } else {
      this.experimentForm.markAllAsTouched();
    }
  }

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
    // TODO: Call backend Archive
    this.notification.success('Experiment Archived', 'Experiment was archived successfully!');
    this.experimentForm.reset();
    this.router.navigate(['/search']);
  }

  closeModals(): void {
    this.isCancelExperimentModalOpen = false;
    this.isArchiveExperimentModalOpen = false;
  }
}
