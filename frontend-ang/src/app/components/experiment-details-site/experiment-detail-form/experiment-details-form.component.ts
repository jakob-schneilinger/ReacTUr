import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NotificationService } from '../../../services/notification.service';

export type FormState = 'create' | 'edit';

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

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private notification: NotificationService,
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.setupReactionAutoFill();
  }

  private initForm(): void {
    const isEditMode = this.formState === 'edit';

    this.experimentForm = this.fb.group({
      // Reactants Array (Initialized with 2 fields based on A + B requirement)
      reactants: this.fb.array([
        this.fb.control({ value: '', disabled: isEditMode }, Validators.required),
        this.fb.control({ value: '', disabled: isEditMode }, Validators.required),
      ]),

      // Required Metadata
      date: ['', Validators.required],
      scientist: ['', Validators.required],
      temperature: ['', Validators.required],
      solvent: ['', Validators.required],
      concentrationA: ['', [Validators.required, Validators.min(0)]],
      concentrationB: ['', [Validators.required, Validators.min(0)]],

      // Optional Metadata
      instrument: [''],
      modality: [''],
      wavelength: [''],
      comment: [''],
    });
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
