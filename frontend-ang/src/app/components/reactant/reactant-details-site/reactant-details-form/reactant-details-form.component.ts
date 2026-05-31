import { Component, EventEmitter, Input, OnInit, Output, SimpleChanges } from '@angular/core';
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
import { Reactant } from '../../../../dtos/reactant';

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
export class ReactantDetailsFormComponent implements OnInit {
  @Input() formState: FormState = 'edit';
  @Input() reactant?: Reactant;

  @Output() smiles = new EventEmitter<string>();

  isDeleteReactantModalOpen = false;
  isCancelReactantModalOpen = false;

  reactantForm!: FormGroup;

  // TODO: fetch from backend
  knownTypes = ['Solvent', 'Catalyst', 'Substrate', 'Reagent'];

  filteredTypes: string[] = [];
  showTypes = false;

  activeTypesIndex = -1;

  rdkitModule: any;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private notification: NotificationService,
  ) {}

  async ngOnInit(): Promise<void> {
    this.initForm();
    this.setupFilters();

    if (!(window as any)['RDKit']) {
      this.rdkitModule = await initRDKitModule();
      (window as any)['RDKit'] = this.rdkitModule;
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

  private setupFilters(): void {
    this.filteredTypes = [...this.knownTypes];

    // Filter Solvents
    this.reactantForm.get('type1')?.valueChanges.subscribe((value) => {
      const searchTerm = (value || '').toLowerCase();
      this.filteredTypes = this.knownTypes.filter((s) => s.toLowerCase().includes(searchTerm));
      this.activeTypesIndex = -1;
    });
  }

  selectOption(controlName: string, value: string): void {
    this.reactantForm.get(controlName)?.setValue(value);
    if (controlName === 'type1') this.showTypes = false;
  }

  onBlur(dropdownName: 'type1'): void {
    setTimeout(() => {
      if (dropdownName === 'type1') this.showTypes = false;
    }, 200);
  }

  onKeyDown(event: KeyboardEvent, dropdown: 'type1'): void {
    let activeIndex: number;
    let filteredList: string[];

    activeIndex = this.activeTypesIndex;
    filteredList = this.filteredTypes;

    if (!filteredList.length) return;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      activeIndex = activeIndex < filteredList.length - 1 ? activeIndex + 1 : activeIndex;
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      activeIndex = activeIndex > 0 ? activeIndex - 1 : 0;
    } else if (event.key === 'Enter') {
      if (activeIndex >= 0) {
        event.preventDefault();
        this.selectOption(dropdown, filteredList[activeIndex]);
        activeIndex = -1;
      }
    }

    if (dropdown === 'type1') this.activeTypesIndex = activeIndex;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.reactantForm) {
      if (changes['reactant'] && this.reactant) {
        this.reactantForm.patchValue(this.reactant);
      }
    }
  }

  private initForm(): void {
    this.reactantForm = this.fb.group({
      // Required Inputs
      shortName: [{ value: '', disabled: this.isEditMode }, Validators.required],
      smiles: ['', [Validators.required, this.smilesValidator()]],
      type1: ['', [Validators.required, this.inListValidator(this.knownTypes)]],

      // Optional Inputs
      chemicalName: [''],
      additionalShortNames: this.fb.array([this.fb.control('')]),
      casNumber: [''],
      references: this.fb.array([this.fb.control('')]),
      comment: [''],
    });

    if (this.reactant) {
      this.syncArray('additionalShortNames', this.reactant.additionalShortNames);
      this.syncArray('references', this.reactant.references);
      this.reactantForm.patchValue(this.reactant);
    }

    this.reactantForm.get('smiles')?.valueChanges.subscribe((value: string) => {
      const smilesControl = this.reactantForm.get('smiles');
      if (smilesControl?.valid) {
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

  inListValidator(validOptions: string[]): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;

      const isValid = validOptions.includes(control.value);

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
    this.reactantForm.reset();
    this.router.navigate(['/reactants']);
  }

  onDelete(): void {
    this.isDeleteReactantModalOpen = true;
  }

  executeDelete(): void {
    this.isDeleteReactantModalOpen = false;
    // TODO: Call backend delete
    this.notification.success(
      'Reactant Removed',
      `The reactant "${this.reactantForm.get('shortName')?.value}" was successfully removed.`,
    );
    this.reactantForm.reset();
    this.router.navigate(['/reactants']);
  }

  closeModals(): void {
    this.isCancelReactantModalOpen = false;
    this.isDeleteReactantModalOpen = false;
  }
}
