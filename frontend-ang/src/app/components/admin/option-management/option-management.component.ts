import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { OptionService } from '../../../services/option.service';
import { SelectionOptionDto } from '../../../dtos/selection-option.dto';
import { NotificationService } from '../../../services/notification.service';
import { TRANSLOCO_IMPORTS } from '../../../global/transloco-imports';

type OptionCategory = 'type' | 'modality' | 'instrument' | 'solvent';

@Component({
  selector: 'app-option-management',
  templateUrl: './option-management.component.html',
  styleUrls: ['./option-management.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, TRANSLOCO_IMPORTS],
})
export class OptionManagementComponent implements OnInit {
  categories: { label: string; value: OptionCategory }[] = [
    { label: 'selectionOptions.type', value: 'type' },
    { label: 'selectionOptions.modality', value: 'modality' },
    { label: 'selectionOptions.instrument', value: 'instrument' },
    { label: 'selectionOptions.solvent', value: 'solvent' },
  ];

  selectedCategory: OptionCategory = 'type';
  options: SelectionOptionDto[] = [];
  newOptionValue: string = '';
  isLoading: boolean = false;

  private optionsCache = new Map<OptionCategory, SelectionOptionDto[]>();

  constructor(
    private optionService: OptionService,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef,
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

  loadOptions(forceRefresh: boolean = false): void {
    if (!forceRefresh) {
      if (this.optionsCache.has(this.selectedCategory)) {
        this.options = this.optionsCache.get(this.selectedCategory) || [];
        return;
      }
      this.isLoading = true;
    }

    this.optionService.getSelectionOptions(this.selectedCategory).subscribe({
      next: (data) => {
        this.options = data;
        this.optionsCache.set(this.selectedCategory, data);
        this.isLoading = false;
        this.cdr.markForCheck();
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
    const newStatus = !!option.archivedAt;
    this.optionService.updateSelectionOptionStatus(option.uuid, newStatus).subscribe({
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

    this.optionService.addSelectionOption(this.selectedCategory, value).subscribe({
      next: (newOption) => {
        this.newOptionValue = '';
        this.loadOptions(true);
        this.notification.success('New option added successfully');
        this.cdr.markForCheck();
      },
      error: (err) =>
        this.notification.error('Failed to add new option', err.message || err.error || err),
    });
  }
}
