import { Component, OnInit } from '@angular/core';
import { OptionService } from '../../../services/option.service';
import { SelectionOptionDto } from '../../../dtos/selection-option.dto';
import { NotificationService } from '../../../services/notification.service';

type OptionCategory = 'type' | 'modality' | 'instrument' | 'solvent';

@Component({
  selector: 'app-option-management',
  templateUrl: './option-management.component.html',
  styleUrls: ['./option-management.component.scss'],
  standalone: false,
})
export class OptionManagementComponent implements OnInit {
  categories: { label: string; value: OptionCategory }[] = [
    { label: 'Type', value: 'type' },
    { label: 'Modality', value: 'modality' },
    { label: 'Instrument', value: 'instrument' },
    { label: 'Solvent', value: 'solvent' },
  ];

  selectedCategory: OptionCategory = 'type';
  options: SelectionOptionDto[] = [];
  newOptionValue: string = '';
  isLoading: boolean = false;

  constructor(
    private optionService: OptionService,
    private notification: NotificationService,
  ) {}

  ngOnInit(): void {
    this.loadOptions();
  }

  selectCategory(category: OptionCategory): void {
    if (this.selectedCategory === category) return;
    this.selectedCategory = category;
    this.newOptionValue = '';
    this.loadOptions();
  }

  loadOptions(): void {
    this.isLoading = true;
    this.optionService.getOptions(this.selectedCategory).subscribe({
      next: (data) => {
        this.options = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.notification.error(
          `Failed to load options for ${this.selectedCategory}`,
          err.message || err.error || err,
        );
        this.isLoading = false;
      },
    });
  }

  toggleOptionStatus(option: SelectionOptionDto): void {
    const newStatus = !option.archivedAt;
    this.optionService.updateKnownOptionStatus(option.uuid, newStatus).subscribe({
      next: (updatedOption) => {
        option.archivedAt = updatedOption.archivedAt;
        this.notification.success('Option status updated successfully');
      },
      error: (err) =>
        this.notification.error('Failed to update option', err.message || err.error || err),
    });
  }

  addOption(): void {
    const value = this.newOptionValue.trim();
    if (!value) return;

    this.optionService.addKnownOption(this.selectedCategory, value).subscribe({
      next: (newOption) => {
        this.loadOptions();
        this.newOptionValue = '';
        this.notification.success('New option added successfully');
      },
      error: (err) =>
        this.notification.error('Failed to add new option', err.message || err.error || err),
    });
  }
}
