import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { OptionService } from '../../../shared/services/option.service';
import { SelectionOptionDto } from '../../../shared/dtos/selection-option.dto';
import { NotificationService } from '../../../core/layout/notification/notification.service';
import { TRANSLOCO_IMPORTS } from '../../../core/global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { AppConfirmModalComponent } from '../../../shared/modal/confirm-modal.component';

type OptionCategory = 'chemical_type' | 'modality' | 'instrument' | 'solvent';

@Component({
  selector: 'app-option-management',
  templateUrl: './option-management.component.html',
  styleUrls: ['./option-management.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, TRANSLOCO_IMPORTS, AppConfirmModalComponent],
})
export class OptionManagementComponent implements OnInit {
  categories: { label: string; value: OptionCategory }[] = [
    { label: 'selectionOptions.type', value: 'chemical_type' },
    { label: 'selectionOptions.modality', value: 'modality' },
    { label: 'selectionOptions.instrument', value: 'instrument' },
    { label: 'selectionOptions.solvent', value: 'solvent' },
  ];

  selectedCategory: OptionCategory = 'chemical_type';
  options: SelectionOptionDto[] = [];
  isAddOptionModalOpen: boolean = false;
  newOptionValue: string = '';
  isLoading: boolean = false;

  private optionsCache = new Map<OptionCategory, SelectionOptionDto[]>();

  constructor(
    private optionService: OptionService,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef,
    private transloco: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.loadOptions();
  }

  capitalizeOption(option: string): string {
    return option.charAt(0).toUpperCase() + option.slice(1);
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
          this.transloco.translate('admin.option.notifications.loadError', {
            category: this.selectedCategory,
          }),
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
        this.notification.success(
          this.transloco.translate('admin.option.notifications.statusUpdated'),
        );
      },
      error: (err) =>
        this.notification.error(
          this.transloco.translate('admin.option.notifications.statusError'),
          err.message || err.error || err,
        ),
    });
  }

  cancelAddOption() {
    this.isAddOptionModalOpen = false;
  }

  addOption(): void {
    if (!this.newOptionValue) return;

    this.isAddOptionModalOpen = true;
  }

  executeAddOption() {
    const value = this.newOptionValue.trim();
    if (!value) return;

    this.optionService.addSelectionOption(this.selectedCategory, value).subscribe({
      next: (newOption) => {
        this.newOptionValue = '';
        this.loadOptions(true);
        this.notification.success(
          this.transloco.translate('admin.option.notifications.addSuccess'),
        );
        this.cancelAddOption();
        this.cdr.markForCheck();
      },
      error: (err) =>
        this.notification.error(
          this.transloco.translate('admin.option.notifications.addError'),
          err.message || err.error || err,
        ),
    });
  }
}
