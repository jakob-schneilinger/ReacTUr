import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ExperimentViewModalComponent } from '../../../experiments/components/experiment-view-modal/experiment-view-modal.component';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { GetExperimentDto } from '../../../experiments/experiment.dto';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { HasRoleDirective } from '../../../../shared/directives/has-role.directive';
import { GetUserDto } from '../../../../shared/dtos/user.dto';

export type SearchState = 'initial' | 'no-results' | 'has-results';

@Component({
  selector: 'app-experiment-results',
  templateUrl: './search-results.component.html',
  styleUrls: ['./search-results.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, ExperimentViewModalComponent, HasRoleDirective],
})
export class SearchResultsComponent {
  @Input() state: SearchState = 'initial';
  @Input() experiments: GetExperimentDto[] = [];
  @Input() currentPage: number = 0;
  @Input() totalElements: number = 0;
  @Input() pageSize: number = 25;

  @Output() pageChange = new EventEmitter<number>();
  @Output() getExport = new EventEmitter<string[]>();
  @Output() searchScientist = new EventEmitter<GetUserDto>();
  @Output() pageSizeChange = new EventEmitter<number>();

  selectedIds = new Set<string>();
  expandedCardIds = new Set<string>();

  isViewModalOpen: boolean = false;
  selectedExperimentForView: GetExperimentDto | undefined;
  hasWritePermission: boolean = false;

  user: GetUserDto | null = null;

  constructor(
    private notification: NotificationService,
    private authService: AuthService,
  ) {}

  get hasNextPage(): boolean {
    return (this.currentPage + 1) * this.pageSize < this.totalElements;
  }

  toggleCardDetails(expId: string, event: Event) {
    event.stopPropagation();
    if (this.expandedCardIds.has(expId)) {
      this.expandedCardIds.delete(expId);
    } else {
      this.expandedCardIds.add(expId);
    }
  }

  isCardExpanded(expId: string): boolean {
    return this.expandedCardIds.has(expId);
  }

  toggleSelection(expId: string) {
    if (this.selectedIds.has(expId)) {
      this.selectedIds.delete(expId);
    } else {
      this.selectedIds.add(expId);
    }
  }

  isSelected(expId: string): boolean {
    return this.selectedIds.has(expId);
  }

  get allSelected(): boolean {
    if (this.experiments.length === 0) return false;
    return this.experiments.every((exp) => this.selectedIds.has(exp.uuid));
  }

  toggleSelectAll() {
    if (this.allSelected) {
      this.experiments.forEach((exp) => this.selectedIds.delete(exp.uuid));
    } else {
      this.experiments.forEach((exp) => this.selectedIds.add(exp.uuid));
    }
  }

  onExportClicked() {
    this.getExport.emit(Array.from(this.selectedIds));
  }

  viewDetails(uuid: string | undefined): void {
    if (!uuid) return;

    this.authService.getLoggedInUser().subscribe((value) => (this.user = value));

    if (!this.user) return;
    this.hasWritePermission = !!this.authService.hasExperimentPermission(uuid, this.user.id);

    const experimentToView = this.experiments.find((exp) => exp.uuid === uuid);

    if (experimentToView) {
      this.selectedExperimentForView = experimentToView;
      this.isViewModalOpen = true;
    } else {
      this.notification.error('Error', 'Experiment details could not be found.');
    }
  }

  closeViewModal(): void {
    this.isViewModalOpen = false;
    this.selectedExperimentForView = undefined;
    this.hasWritePermission = false;
  }

  searchForScientist(scientist: GetUserDto) {
    this.searchScientist.emit(scientist);
  }

  onPageSizeChange(event: Event) {
    const selectElement = event.target as HTMLSelectElement;
    const newSize = Number(selectElement.value);
    this.pageSizeChange.emit(newSize);
  }
}
