import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ExperimentViewModalComponent } from '../../../experiments/components/experiment-view-modal/experiment-view-modal.component';
import { Component, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { GetExperimentDto } from '../../../experiments/experiment.dto';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { HasRoleDirective } from '../../../../shared/directives/has-role.directive';
import { GetUserDto } from '../../../../shared/dtos/user.dto';
import { ExperimentService } from '../../../experiments/experiment.service';
import { TranslocoService } from '@jsverse/transloco';
import { AppConfirmModalComponent } from '../../../../shared/modal/confirm-modal.component';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';

export type SearchState = 'initial' | 'no-results' | 'has-results';

@Component({
  selector: 'app-search-results',
  templateUrl: './search-results.component.html',
  styleUrls: ['./search-results.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ExperimentViewModalComponent,
    HasRoleDirective,
    AppConfirmModalComponent,
    TRANSLOCO_IMPORTS,
  ],
})
export class SearchResultsComponent {
  @Input() disableExport: boolean = false;
  @Input() allowArchive: boolean = false;
  @Input() showPages: boolean = true;
  @Input() state: SearchState = 'initial';
  @Input() experiments: GetExperimentDto[] = [];
  @Input() currentPage: number = 0;
  @Input() totalElements: number = 0;
  @Input() pageSize: number = 25;

  @Output() listChanged = new EventEmitter<void>();
  @Output() pageChange = new EventEmitter<number>();
  @Output() getExport = new EventEmitter<GetExperimentDto[]>();
  @Output() getExportAll = new EventEmitter<void>();
  @Output() searchScientist = new EventEmitter<GetUserDto>();
  @Output() pageSizeChange = new EventEmitter<number>();

  selectedExperiments = new Map<string, GetExperimentDto>();
  expandedCardIds = new Set<string>();

  user: GetUserDto | null = null;

  isViewModalOpen: boolean = false;
  selectedExperimentForView: GetExperimentDto | undefined;
  hasWritePermission: boolean = false;

  restoreExperimentUuid: string = '';
  isRestoreExperimentModalOpen: boolean = false;

  archiveExperimentUuid: string = '';
  isArchiveExperimentModalOpen: boolean = false;

  constructor(
    private notification: NotificationService,
    private authService: AuthService,
    private experimentService: ExperimentService,
    private transloco: TranslocoService,
    private cdr: ChangeDetectorRef,
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

  toggleSelection(exp: GetExperimentDto) {
    if (this.selectedExperiments.has(exp.uuid)) {
      this.selectedExperiments.delete(exp.uuid);
    } else {
      this.selectedExperiments.set(exp.uuid, exp);
    }
  }

  isSelected(uuid: string): boolean {
    return this.selectedExperiments.has(uuid);
  }

  get allSelected(): boolean {
    if (this.experiments.length === 0) return false;
    return this.experiments.every((exp) => this.selectedExperiments.has(exp.uuid));
  }

  toggleSelectAll() {
    if (this.allSelected) {
      this.experiments.forEach((exp) => this.selectedExperiments.delete(exp.uuid));
    } else {
      this.experiments.forEach((exp) => this.selectedExperiments.set(exp.uuid, exp));
    }
  }

  onExportClicked() {
    this.getExport.emit(Array.from(this.selectedExperiments.values()));
  }

  onExportAllClicked() {
    this.getExportAll.emit();
  }

  viewDetails(uuid: string | undefined): void {
    if (!uuid) return;

    this.authService.getLoggedInUser().subscribe((value) => (this.user = value));

    if (!this.user) return;
    const userId = this.user.id;

    const experimentToView = this.experiments.find((exp) => exp.uuid === uuid);

    if (experimentToView) {
      this.hasWritePermission =
        this.user.role == 'ADMIN' ||
        experimentToView.createdBy.id === userId ||
        experimentToView.metadata.scientist.id === userId;
      this.selectedExperimentForView = experimentToView;
      this.isViewModalOpen = true;
    } else {
      this.notification.error(
        this.transloco.translate('search.notifications.errorTitle'),
        this.transloco.translate('search.notifications.experimentDetailsErrorMessage'),
      );
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

  cancelRestore() {
    this.isRestoreExperimentModalOpen = false;
    this.restoreExperimentUuid = '';
  }

  restoreExperiment(uuid: string): void {
    if (!uuid) return;

    this.restoreExperimentUuid = uuid;
    this.isRestoreExperimentModalOpen = true;
  }

  executeRestore() {
    if (!this.restoreExperimentUuid) return;
    this.experimentService.restoreExperiment(this.restoreExperimentUuid).subscribe({
      next: (value) => {
        this.notification.success(
          this.transloco.translate('search.notifications.restoreTitle'),
          this.transloco.translate('search.notifications.restoreMessage'),
        );
        this.experiments = this.experiments.filter(
          (exp) => exp.uuid !== this.restoreExperimentUuid,
        );
        this.cancelRestore();
        this.listChanged.emit();
      },
      error: (err) => {
        this.notification.error(
          this.transloco.translate('search.notifications.errorTitle'),
          this.transloco.translate('search.notifications.restoreErrorMessage'),
        );
        this.cancelRestore();
        this.cdr.markForCheck();
      },
    });
  }

  cancelArchive() {
    this.isArchiveExperimentModalOpen = false;
    this.archiveExperimentUuid = '';
  }

  archiveExperiment(uuid: string): void {
    if (!uuid) return;
    this.archiveExperimentUuid = uuid;
    this.isArchiveExperimentModalOpen = true;
  }

  executeArchive() {
    if (!this.archiveExperimentUuid) return;
    this.experimentService.archiveExperiment(this.archiveExperimentUuid).subscribe({
      next: () => {
        this.notification.success(
          this.transloco.translate('search.notifications.archiveTitle'),
          this.transloco.translate('search.notifications.archiveMessage'),
        );
        this.cancelArchive();
        this.listChanged.emit();
      },
      error: () => {
        this.notification.error(
          this.transloco.translate('search.notifications.errorTitle'),
          this.transloco.translate('search.notifications.archiveErrorMessage'),
        );
        this.cancelArchive();
        this.cdr.markForCheck();
      },
    });
  }
}
