import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { GetExperimentDto } from '../../../dtos/experiment.dto';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth.service';

export type SearchState = 'initial' | 'no-results' | 'has-results';


@Component({
  selector: 'app-experiment-results',
  templateUrl: './search-results.component.html',
  styleUrls: ['./search-results.component.scss'],
  standalone: false,
})
export class SearchResultsComponent implements OnChanges {
  @Input() state: SearchState = 'initial';
  @Input() experiments: GetExperimentDto[] = [];
  @Output() getExport = new EventEmitter<string[]>();

  selectedIds = new Set<string>();
  isViewModalOpen:boolean = false;
  selectedExperimentForView: GetExperimentDto | undefined;
  hasWritePermission: boolean = false;

  constructor(
    private notification: NotificationService,
    private authService: AuthService
  ) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['experiments']) {
      this.cleanupSelection();
    }
  }

  private cleanupSelection(): void {
    if (!this.experiments || this.experiments.length === 0) {
      this.selectedIds.clear();
      return;
    }

    const visibleIds = new Set(this.experiments.map((exp) => exp.uuid));

    for (const id of this.selectedIds) {
      if (!visibleIds.has(id)) {
        this.selectedIds.delete(id);
      }
    }
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

  viewDetails(uuid:string | undefined): void {
    if (!uuid) return;

    const user = this.authService.getLoggedInUser();

    if (!user) return;
    this.hasWritePermission = !!this.authService.hasExperimentPermission(uuid, user.uuid);

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
}
