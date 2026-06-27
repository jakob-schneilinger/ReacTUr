import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';

@Component({
  selector: 'app-option-selector',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './option-selector.component.html',
  styleUrls: ['./option-selector.component.scss'],
})
export class OptionSelectorComponent {
  @Input({ required: true }) control!: FormControl;
  @Input({ required: true }) options: any[] = [];
  @Input() label: string = '';
  @Input() placeholder: string = 'Select...';
  @Input() datalistId: string = 'options-list';
  @Input() errorMessage: string = 'Field is required.';

  clearControl(): void {
    this.control.setValue('');
  }
}
