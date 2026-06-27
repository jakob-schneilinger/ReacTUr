import { Component, Input, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormArray, FormBuilder, Validators, FormControl } from '@angular/forms';
import { NotificationService } from '../../../services/notification.service';

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

  constructor(
    private fb: FormBuilder,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef,
  ) {}

  get controls() {
    return this.formArray.controls as FormControl[];
  }

  addArrayField(): void {
    const lastControl = this.formArray.at(this.formArray.length - 1);
    if (lastControl && !lastControl.value) {
      this.notification.error(
        'Field is empty',
        'Please fill out the last field before adding a new one.',
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
