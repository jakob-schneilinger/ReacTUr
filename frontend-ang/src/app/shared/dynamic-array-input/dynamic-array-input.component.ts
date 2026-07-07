import { Component, Input, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormArray, FormBuilder, Validators, FormControl } from '@angular/forms';
import { NotificationService } from '../../core/layout/notification/notification.service';
import { TranslocoService } from '@jsverse/transloco';

@Component({
  selector: 'app-dynamic-array-input',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './dynamic-array-input.component.html',
  styleUrls: ['./dynamic-array-input.component.scss'],
})
export class DynamicArrayInputComponent {
  @Input({ required: true }) formArray!: FormArray;
  @Input() arrayName: string = '';
  @Input() label: string = '';
  @Input() addButtonLabel: string = 'Add field';
  @Input() placeholder: string = 'Value';

  errorMessage?: string;

  constructor(
    private fb: FormBuilder,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef,
    private transloco: TranslocoService
  ) {}

  get controls() {
    return this.formArray.controls as FormControl[];
  }

  addArrayField(): void {
    const lastControl = this.formArray.at(this.formArray.length - 1);
    if (lastControl && !lastControl.value) {
      this.notification.error(
        this.transloco.translate('reactants.details.notifications.fieldEmptyErrorTitle'),
        this.transloco.translate('reactants.details.notifications.fieldEmptyErrorMessage'),
      );
      return;
    }
    this.formArray.push(this.fb.control(''));
    this.cdr.markForCheck();

    setTimeout(() => {
      const index = this.formArray.length - 1;
      const inputId = `${this.arrayName}-${index}`;
      const inputElement = document.getElementById(inputId);
      if (inputElement) {
        inputElement.focus();
      }
    }, 0);
  }

  removeArrayField(index: number): void {
    if (this.formArray.length > 1) {
      this.formArray.removeAt(index);
      this.cdr.markForCheck();
    }
  }

  clearArrayControl(index: number): void {
    this.formArray.at(index).setValue('');
  }
}
