import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { AppConfirmModalComponent } from '../../../shared/modal/confirm-modal.component';
import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, ValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { NotificationService } from '../../../../services/notification.service';
import { AuthService } from '../../../../services/auth.service';
import { GetUserDto } from '../../../../dtos/user.dto';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-experiment-details-form',
  templateUrl: './experiment-details-form.component.html',
  styleUrls: ['./experiment-details-form.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppConfirmModalComponent],
})
export class ExperimentDetailsFormComponent implements OnInit {
  @Input() formState: FormState = 'create';

  isArchiveExperimentModalOpen = false;
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

  user: GetUserDto | null = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private notification: NotificationService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getLoggedInUser();
    this.initForm();
    this.setupReactionAutoFill();
    this.setupFilters();
  }

  get fullName(): string {
    return '' + this.user?.firstName + ' ' + this.user?.lastName;
  }

  get currentDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  private initForm(): void {
    const isEditMode = this.formState !== 'create';

    this.experimentForm = this.fb.group({
      // Reactants Array
      reactants: this.fb.array([
        this.fb.control({ value: '', disabled: isEditMode }, Validators.required),
        this.fb.control({ value: '', disabled: isEditMode }, Validators.required),
      ]),

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
      wavelength: [''],
      comment: [''],
    });

    //TODO: if edit mode init form with existing data
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
