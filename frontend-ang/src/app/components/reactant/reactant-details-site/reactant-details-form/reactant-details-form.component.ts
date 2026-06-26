import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { DynamicArrayInputComponent } from '../../../shared/dynamic-array-input/dynamic-array-input.component';
import { OptionSelectorComponent } from '../../../shared/option-selector/option-selector.component';
import { AppConfirmModalComponent } from '../../../shared/modal/confirm-modal.component';
import { HasRoleDirective } from '../../../../directives/has-role.directive';
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
import { ReactantDetailDto } from '../../../../dtos/reactant.dto';
import { OptionService } from '../../../../services/option.service';
import { ChemicalTypeDto } from '../../../../dtos/selection-option.dto';
import { Subject } from 'rxjs';
import { ReactantService } from '../../../../services/reactant.service';
import { TRANSLOCO_IMPORTS } from '../../../../global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { RDKitService } from '../../../../services/rdkit.service';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-reactant-details-form',
  templateUrl: './reactant-details-form.component.html',
  styleUrls: [
    './reactant-details-form.component.scss',
    '../../../experiment/experiment-details-site/experiment-detail-form/experiment-details-form.component.scss',
  ],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DynamicArrayInputComponent,
    OptionSelectorComponent,
    AppConfirmModalComponent,
    HasRoleDirective,
    TRANSLOCO_IMPORTS
  ],
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

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private notification: NotificationService,
    private optionService: OptionService,
    private reactantService: ReactantService,
    private rdkitService: RDKitService,
    private cdr: ChangeDetectorRef,
    private transloco: TranslocoService,
  ) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  async ngOnInit(): Promise<void> {
    this.optionService.getSelectionOptions('chemical_type').subscribe({
      next: (params) => {
        this.chemicalTypes = params;
        this.chemicalTypeNames = this.chemicalTypes.map((t) => t.name);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.notification.error(
          this.transloco.translate('reactants.details.notifications.typeLoadErrorTitle'),
          this.transloco.translate('reactants.details.notifications.typeLoadErrorMessage', {
            error: err.message || err.error || err,
          }),
        );
      },
    });

    this.initForm();

    await this.rdkitService.initRDKit();
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

  get chemicalTypeControl(): FormControl {
    return this.reactantForm.get('chemicalType') as FormControl;
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

      const status = this.rdkitService.validateSmiles(control.value);
      if (status === 'syntaxError') return { syntaxError: { value: control.value } };
      if (status === 'sanitizationError') return { sanitizationError: { value: control.value } };

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
      data.forEach((item) => formArray.push(this.fb.control(item, Validators.required)));
    } else {
      formArray.push(this.fb.control('', Validators.required));
    }
  }

  clearControl(controlName: string): void {
    this.reactantForm.get(controlName)?.setValue('');
    this.reactantForm.markAsDirty();
  }

  isInvalid(controlName: string): boolean {
    const control = this.reactantForm.get(controlName);
    return !!(control && control.invalid && control.touched);
  }

  onSubmit(): void {
    if (this.reactantForm.valid) {
      const formValue = this.reactantForm.getRawValue();
      formValue.chemicalType = this.chemicalTypes.find((t) => t.name === formValue.chemicalType)?.uuid;

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
                error: err.message || err.error || err,
              }),
            )
          }
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
                error: err.message || err.error || err,
              }),
            ),
        });
      }
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
            this.transloco.translate('reactants.details.notifications.archivedTitle'),
            this.transloco.translate('reactants.details.notifications.archivedMessage', {
              name: this.reactantForm.get('shortName')?.value,
            }),
          );
          this.reactantForm.reset();
          this.router.navigate(['/reactants']);
        },
        error: (err) =>
          this.notification.error(
            this.transloco.translate('reactants.details.notifications.archiveFailedTitle'),
            this.transloco.translate('reactants.details.notifications.archiveFailedMessage', {
              name: this.reactantForm.get('shortName')?.value,
              error: err.message || err.error || err,
            }),
          ),
      });
    }
  }

  closeModals(): void {
    this.isCancelReactantModalOpen = false;
    this.isArchiveReactantModalOpen = false;
  }
}
