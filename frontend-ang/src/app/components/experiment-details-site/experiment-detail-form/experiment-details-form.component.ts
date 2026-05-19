import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, ValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { NotificationService } from '../../../services/notification.service';

export type FormState = 'create' | 'edit' | 'view';

@Component({
  selector: 'app-experiment-details-form',
  templateUrl: './experiment-details-form.component.html',
  styleUrls: ['./experiment-details-form.component.scss'],
  standalone: false,
})
export class ExperimentDetailsFormComponent implements OnInit {
  @Input() formState: FormState = 'create';

  isDeleteExperimentModalOpen = false;
  isCancelExperimentModalOpen = false;

  experimentForm!: FormGroup;

  // TODO: fetch from backend
  knownSolvents = ['Water', 'Methanol', 'Ethanol', 'DMSO', 'Acetonitrile'];
  knownInstruments = ['Spectrometer A', 'HPLC B', 'Mass Spec C'];
  knownModalities = ['UV-Vis', 'Fluorescence', 'NMR'];

  filteredSolvents: string[] = [];
  showSolvents = false;
  filteredInstruments: string[] = [];
  showInstruments = false;
  filteredModalities: string[] = [];
  showModalities = false;

  activeSolventIndex = -1;
  activeInstrumentIndex = -1;
  activeModalityIndex = -1;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private notification: NotificationService,
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.setupReactionAutoFill();
    this.setupFilters();
  }

  private initForm(): void {
    const isEditMode = this.formState !== 'create';
    const isViewMode = this.formState === 'view';

    this.experimentForm = this.fb.group({
      // Reactants Array
      reactants: this.fb.array([
        this.fb.control({ value: '', disabled: isEditMode }, Validators.required),
        this.fb.control({ value: '', disabled: isEditMode }, Validators.required),
      ]),

      // Required Metadata
      date: ['', Validators.required],
      scientist: ['', Validators.required],
      temperature: ['', Validators.required],
      solvent: ['', [Validators.required, this.inListValidator(this.knownSolvents)]],
      concentrationA: ['', [Validators.required, Validators.min(0)]],
      concentrationB: ['', [Validators.required, Validators.min(0)]],

      // Optional Metadata
      instrument: ['', [Validators.required, this.inListValidator(this.knownInstruments)]],
      modality: ['', [Validators.required, this.inListValidator(this.knownModalities)]],
      wavelength: [''],
      comment: [''],
    });

    if (isViewMode) {
      this.experimentForm.disable();
    }
  }

  private inListValidator(validOptions: string[]): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;

      const isValid = validOptions.includes(control.value);

      return isValid ? null : { notInList: { value: control.value } };
    };
  }

  private setupFilters(): void {
    this.filteredSolvents = [...this.knownSolvents];
    this.filteredInstruments = [...this.knownInstruments];
    this.filteredModalities = [...this.knownModalities];

    // Filter Solvents
    this.experimentForm.get('solvent')?.valueChanges.subscribe((value) => {
      const searchTerm = (value || '').toLowerCase();
      this.filteredSolvents = this.knownSolvents.filter((s) =>
        s.toLowerCase().includes(searchTerm),
      );
      this.activeSolventIndex = -1;
    });

    this.experimentForm.get('instrument')?.valueChanges.subscribe((value) => {
      const searchTerm = (value || '').toLowerCase();
      this.filteredInstruments = this.knownInstruments.filter((i) =>
        i.toLowerCase().includes(searchTerm),
      );
      this.activeInstrumentIndex = -1;
    });

    this.experimentForm.get('modality')?.valueChanges.subscribe((value) => {
      const searchTerm = (value || '').toLowerCase();
      this.filteredModalities = this.knownModalities.filter((m) =>
        m.toLowerCase().includes(searchTerm),
      );
      this.activeModalityIndex = -1;
    });
  }

  selectOption(controlName: string, value: string): void {
    this.experimentForm.get(controlName)?.setValue(value);
    if (controlName === 'solvent') this.showSolvents = false;
    if (controlName === 'instrument') this.showInstruments = false;
    if (controlName === 'modality') this.showModalities = false;
  }

  onBlur(dropdownName: 'solvent' | 'instrument' | 'modality'): void {
    setTimeout(() => {
      if (dropdownName === 'solvent') this.showSolvents = false;
      if (dropdownName === 'instrument') this.showInstruments = false;
      if (dropdownName === 'modality') this.showModalities = false;
    }, 200);
  }

  onKeyDown(event: KeyboardEvent, dropdown: 'solvent' | 'instrument' | 'modality'): void {
    let activeIndex: number;
    let filteredList: string[];

    if (dropdown === 'solvent') {
      activeIndex = this.activeSolventIndex;
      filteredList = this.filteredSolvents;
    } else if (dropdown === 'instrument') {
      activeIndex = this.activeInstrumentIndex;
      filteredList = this.filteredInstruments;
    } else {
      activeIndex = this.activeModalityIndex;
      filteredList = this.filteredModalities;
    }

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

    if (dropdown === 'solvent') this.activeSolventIndex = activeIndex;
    if (dropdown === 'instrument') this.activeInstrumentIndex = activeIndex;
    if (dropdown === 'modality') this.activeModalityIndex = activeIndex;
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

  clearReactant(index: number): void {
    const reactantsArray = this.experimentForm.get('reactants') as FormArray;
    reactantsArray.at(index).setValue('');
  }

  clearControl(controlName: string): void {
    this.experimentForm.get(controlName)?.setValue('');
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
    if (this.experimentForm.valid) {
      const formValue = this.experimentForm.getRawValue();
      // TODO: send to backend and wait for success, then ->
      this.notification.success('Experiment Saved', 'Experiment was saved successfully!');
      this.experimentForm.markAsPristine();
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
    this.router.navigate(['/experiments']);
  }

  onDelete(): void {
    this.isDeleteExperimentModalOpen = true;
  }

  executeDelete(): void {
    this.isDeleteExperimentModalOpen = false;
    // TODO: Call backend delete
    this.notification.success('Experiment Deleted', 'Experiment was deleted successfully!');
    this.experimentForm.reset();
    this.router.navigate(['/experiments']);
  }

  closeModals(): void {
    this.isCancelExperimentModalOpen = false;
    this.isDeleteExperimentModalOpen = false;
  }
}
