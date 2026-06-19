import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  FormArray,
  Validators,
  ValidatorFn,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router } from '@angular/router';
import { NotificationService } from '../../../../services/notification.service';
import { ReactantDetailDto, ReactantDto } from '../../../../dtos/reactant.dto';
import { OptionService } from '../../../../services/option.service';
import { ChemicalTypeDto } from '../../../../dtos/selection-option.dto';
import { Subject, takeUntil } from 'rxjs';
import { ReactantService } from '../../../../services/reactant.service';

export type FormState = 'create' | 'edit';

declare var initRDKitModule: any;

@Component({
  selector: 'app-reactant-details-form',
  templateUrl: './reactant-details-form.component.html',
  styleUrls: [
    './reactant-details-form.component.scss',
    '../../../experiment/experiment-details-site/experiment-detail-form/experiment-details-form.component.scss',
  ],
  standalone: false,
})
export class ReactantDetailsFormComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  @Input() formState: FormState = 'edit';
  @Input() reactant?: ReactantDetailDto;

  @Output() smiles = new EventEmitter<string>();

  isArchiveReactantModalOpen = false;
  isCancelReactantModalOpen = false;

  reactantForm!: FormGroup;

  // TODO: fetch from backend
  chemicalTypes: ChemicalTypeDto[] = [];
  chemicalTypeNames: string[] = [];

  rdkitModule: any;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private notification: NotificationService,
    private optionService: OptionService,
    private reactantService: ReactantService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  async ngOnInit(): Promise<void> {
    this.optionService.getOptions('type').subscribe({
      next: (params) => {
        this.chemicalTypes = params;
        this.chemicalTypeNames = this.chemicalTypes.map((t) => t.name);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.notification.error(
          'Type Loading Error',
          `Please refresh page! Error: ${err.message || err.error || err}`,
        );
      },
    });

    this.initForm();

    if (!(window as any)['RDKit']) {
      this.rdkitModule = await initRDKitModule();
      (window as any)['RDKit'] = this.rdkitModule;
      this.reactantForm?.get('smiles')?.updateValueAndValidity();
    } else {
      this.rdkitModule = (window as any)['RDKit'];
    }
  }

  get isEditMode(): boolean {
    return this.formState === 'edit';
  }

  get additionalShortNames(): FormArray {
    return this.reactantForm.get('additionalShortNames') as FormArray;
  }

  get references(): FormArray {
    return this.reactantForm.get('references') as FormArray;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.reactantForm && changes['reactant'] && this.reactant) {
      const currentType = this.reactantForm.get('chemicalType')?.value;

      if (currentType !== this.reactant.chemicalType.name) {
        this.reactantForm.patchValue({
          ...this.reactant,
          chemicalType: this.reactant.chemicalType.name,
        });
      }
    }
  }

  private initForm(): void {
    const initialType = this.reactant?.chemicalType?.name || '';

    this.reactantForm = this.fb.group({
      // Required Inputs
      shortName: [
        { value: this.reactant?.shortName || '', disabled: this.isEditMode },
        Validators.required,
      ],
      smiles: [this.reactant?.smiles || '', [Validators.required, this.smilesValidator()]],
      chemicalType: [initialType, [Validators.required, this.inListValidator()]],

      // Optional Inputs
      chemicalName: [this.reactant?.chemicalName || ''],
      casNumber: [this.reactant?.casNumber || ''],
      comment: [this.reactant?.comment || ''],

      additionalShortNames: this.fb.array([]),
      references: this.fb.array([]),
    });

    if (this.reactant) {
      this.syncArray('additionalShortNames', this.reactant.additionalShortNames);
      this.syncArray('references', this.reactant.references);
    } else {
      this.additionalShortNames.push(this.fb.control(''));
      this.references.push(this.fb.control(''));
    }

    this.reactantForm.get('smiles')?.valueChanges.subscribe((value: string) => {
      if (value !== null) {
        this.smiles.emit(value);
      }
    });
  }

  smilesValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      const smiles = control.value;

      if (!this.rdkitModule) {
        console.warn('RDKit not initialized yet');
        return null;
      }

      const options = JSON.stringify({ sanitize: false });
      let unsanitizedMol;

      try {
        unsanitizedMol = this.rdkitModule.get_mol(smiles, options);
      } catch (e) {
        return { syntaxError: { value: smiles } };
      }

      if (
        !unsanitizedMol ||
        (typeof unsanitizedMol.is_valid === 'function' && !unsanitizedMol.is_valid())
      ) {
        if (unsanitizedMol) unsanitizedMol.delete();
        return { syntaxError: { value: smiles } };
      }

      unsanitizedMol.delete();
      let sanitizedMol;

      try {
        sanitizedMol = this.rdkitModule.get_mol(smiles);
      } catch (e) {
        return { sanitizationError: { value: smiles } };
      }

      if (
        !sanitizedMol ||
        (typeof sanitizedMol.is_valid === 'function' && !sanitizedMol.is_valid())
      ) {
        if (sanitizedMol) sanitizedMol.delete();
        return { sanitizationError: { value: smiles } };
      }

      sanitizedMol.delete();
      return null;
    };
  }

  inListValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;

      const isValid = this.chemicalTypeNames.includes(control.value);

      return isValid ? null : { notInList: { value: control.value } };
    };
  }

  private syncArray(arrayName: 'additionalShortNames' | 'references', data?: string[]): void {
    const formArray = this.reactantForm.get(arrayName) as FormArray;
    formArray.clear();
    if (data && data.length > 0) {
      data.forEach((item) => formArray.push(this.fb.control(item)));
    } else {
      formArray.push(this.fb.control(''));
    }
  }

  clearControl(controlName: string): void {
    this.reactantForm.get(controlName)?.setValue('');
    this.reactantForm.markAsDirty();
  }

  clearArrayControl(arrayName: 'additionalShortNames' | 'references', index: number): void {
    const array = this.reactantForm.get(arrayName) as FormArray;
    array.at(index).setValue('');
    this.reactantForm.markAsDirty();
  }

  addArrayField(arrayName: 'additionalShortNames' | 'references'): void {
    const array = this.reactantForm.get(arrayName) as FormArray;
    array.push(this.fb.control(''));
    this.reactantForm.markAsDirty();

    setTimeout(() => {
      const newIndex = array.length - 1;
      const inputId = `${arrayName}-${newIndex}`;
      const inputElement = document.getElementById(inputId);
      if (inputElement) {
        inputElement.focus();
      }
    });
  }

  removeArrayField(arrayName: 'additionalShortNames' | 'references', index: number): void {
    const array = this.reactantForm.get(arrayName) as FormArray;
    array.removeAt(index);
    if (array.length === 0) {
      array.push(this.fb.control(''));
    }
    this.reactantForm.markAsDirty();
  }

  isInvalid(controlName: string): boolean {
    const control = this.reactantForm.get(controlName);
    return !!(control && control.invalid && control.touched);
  }

  onSubmit(): void {
    if (this.reactantForm.valid) {
      const formValue = this.reactantForm.getRawValue();
      // TODO: send to backend (create or edit based on formState)
      this.notification.success('Reactant Saved', 'Reactant was saved successfully!');
      this.reactantForm.markAsPristine();
    } else {
      this.reactantForm.markAllAsTouched();
    }
  }

  onCancel(): void {
    if (this.reactantForm.dirty) {
      this.isCancelReactantModalOpen = true;
    } else {
      this.executeCancel();
    }
  }

  executeCancel(): void {
    this.isCancelReactantModalOpen = false;
    this.reactantForm.reset(undefined, { emitEvent: false });
    this.router.navigate(['/reactants']);
  }

  onArchive(): void {
    this.isArchiveReactantModalOpen = true;
  }

  executeArchive(): void {
    this.isArchiveReactantModalOpen = false;
    // TODO: Call backend Archive
    if (this.reactant) {
      this.reactantService.archiveReactant(this.reactant.uuid).subscribe({
        next: () => {
          this.notification.success(
            'Reactant Archived',
            `The reactant "${this.reactantForm.get('shortName')?.value}" was successfully archived.`,
          );
          this.reactantForm.reset();
          this.router.navigate(['/reactants']);
        },
        error: (err) =>
          this.notification.error(
            'Archive Failed',
            `Archiving of Reactant "${this.reactantForm.get('shortName')?.value}" failed! Error: ${err.message || err.error || err}`,
          ),
      });
    }
  }

  closeModals(): void {
    this.isCancelReactantModalOpen = false;
    this.isArchiveReactantModalOpen = false;
  }
}
