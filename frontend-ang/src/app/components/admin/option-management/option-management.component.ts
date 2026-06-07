import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../../services/admin.service';
import { KnownOption } from '../../../dtos/known-option';
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
  options: KnownOption[] = [];
  newOptionValue: string = '';
  isLoading: boolean = false;

  constructor(
    private adminService: AdminService,
    private notifications: NotificationService,
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
    this.adminService.getKnownOptions(this.selectedCategory).subscribe({
      next: (data) => {
        this.options = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.notifications.error(`Failed to load options for ${this.selectedCategory}`, err);
        this.isLoading = false;
      },
    });
  }

  toggleOptionStatus(option: KnownOption): void {
    const newStatus = !option.isActive;
    this.adminService.updateKnownOptionStatus(option.id, newStatus).subscribe({
      next: (updatedOption) => {
        option.isActive = updatedOption.isActive;
        this.notifications.success('Option status updated successfully');
      },
      error: (err) => this.notifications.error('Failed to update option', err),
    });
  }

  addOption(): void {
    const value = this.newOptionValue.trim();
    if (!value) return;

    this.adminService.addKnownOption(this.selectedCategory, value).subscribe({
      next: (newOption) => {
        this.loadOptions();
        this.newOptionValue = '';
        this.notifications.success('New option added successfully');
      },
      error: (err) => this.notifications.error('Failed to add new option', err),
    });
  }
}
