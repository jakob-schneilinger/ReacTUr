import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReactantSearchInputComponent } from '../../components/reactant-search-input/reactant-search-input.component';
import { ReactantCardComponent } from '../../components/reactant-card/reactant-card.component';
import { AppConfirmModalComponent } from '../../../../shared/modal/confirm-modal.component';
import { ReactantViewModalComponent } from '../../components/reactant-view-modal/reactant-view-modal.component';
import { DrawMolComponent } from '../../../../shared/draw-mol/draw-mol.component';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ReactantDto } from '../../reactant.dto';
import { ReactantService } from '../../reactant.service';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { HasRoleDirective } from '../../../../shared/directives/has-role.directive';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { StructureFlag } from '../../structure-flag.enum';

@Component({
  selector: 'app-reactant-overview-page',
  templateUrl: './reactant-overview-page.component.html',
  styleUrls: ['./reactant-overview-page.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactantSearchInputComponent,
    ReactantCardComponent,
    AppConfirmModalComponent,
    ReactantViewModalComponent,
    DrawMolComponent,
    HasRoleDirective,
    TRANSLOCO_IMPORTS,
  ],
})
export class ReactantOverviewPageComponent implements OnInit {
  reactants: ReactantDto[] = [];
  filteredReactants: ReactantDto[] = [];
  searchTerm: string = '';

  archivedReactants: ReactantDto[] = [];
  showArchived: boolean = false;

  activeReactants: ReactantDto[] = [];

  role: string = 'USER';
  hasWritePermission: boolean = false;

  isArchiveReactantModalOpen: boolean = false;
  pendingArchiveReactant: ReactantDto | undefined = undefined;

  isRestoreReactantModalOpen: boolean = false;
  pendingRestoreReactant: ReactantDto | undefined = undefined;

  isViewModalOpen: boolean = false;
  selectedReactantForView: ReactantDto | undefined = undefined;

  isDrawModalOpen = false;
  currentSmilesForModal = '';

  constructor(
    private router: Router,
    private reactantService: ReactantService,
    private authService: AuthService,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef,
    private transloco: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.authService.getLoggedInUser().subscribe((user) => {
      if (user) {
        this.role = user.role;
        this.hasWritePermission = this.role !== 'USER';
        this.loadReactants();
        this.cdr.markForCheck();
      }
    });
  }

  private loadReactants() {
    this.reactantService.getAllReactants().subscribe({
      next: (value) => {
        if (this.role === 'USER') {
          this.reactants = value.filter((r) => r.flag === StructureFlag.ACTIVE);
        } else {
          this.reactants = value;
        }
        if (!this.showArchived) {
          this.activeReactants = this.reactants;
          this.onSearch();
          this.cdr.markForCheck();
        }
      },
      error: (err) =>
        this.notification.error(
          this.transloco.translate('reactants.notifications.fetchError'),
          err.message || err.error || err,
        ),
    });

    if (this.role === 'ADMIN') {
      this.reactantService.getArchivedReactants().subscribe({
        next: (value) => {
          this.archivedReactants = value;
          if (this.showArchived) {
            this.activeReactants = this.archivedReactants;
            this.onSearch();
            this.cdr.markForCheck();
          }
        },
        error: (err) =>
          this.notification.error(
            this.transloco.translate('reactants.notifications.fetchArchivedError'),
            err.message || err.error || err,
          ),
      });
    }
  }

  showArchivedReactants(show: boolean) {
    this.showArchived = show;
    this.activeReactants = show ? this.archivedReactants : this.reactants;
    this.onSearch();
  }

  onSearch(): void {
    const term = this.searchTerm.toLowerCase().trim();
    if (!term) {
      this.filteredReactants = [...this.activeReactants];
      return;
    }

    this.filteredReactants = this.activeReactants.filter(
      (reactant) =>
        reactant.shortName.toLowerCase().includes(term) ||
        reactant.smiles.toLowerCase().includes(term) ||
        (reactant.chemicalName && reactant.chemicalName.toLowerCase().includes(term)) ||
        reactant.chemicalType?.name.toLowerCase().includes(term), //TODO: chemicalType will mandatory in future
    );
  }

  goToCreate(): void {
    this.router.navigate(['/reactants/create']);
  }

  viewDetails(uuid: string | undefined, isArchived: boolean): void {
    if (!uuid) return;

    const tempReactants = isArchived ? this.archivedReactants : this.reactants;
    const reactantToView = tempReactants.find((r) => r.uuid === uuid);

    if (reactantToView) {
      this.selectedReactantForView = reactantToView;
      this.isViewModalOpen = true;
    } else {
      this.notification.error(
        this.transloco.translate('reactants.notifications.notFoundTitle'),
        this.transloco.translate('reactants.notifications.notFoundMessage'),
      );
    }
  }

  closeViewModal(): void {
    this.isViewModalOpen = false;
    this.selectedReactantForView = undefined;
  }

  editReactant(uuid: string | undefined): void {
    if (uuid) this.router.navigate(['/reactants', uuid]);
  }

  cancelArchive() {
    this.isArchiveReactantModalOpen = false;
    this.pendingArchiveReactant = undefined;
  }

  archiveReactant(reactant: ReactantDto | undefined): void {
    if (!reactant) return;

    this.pendingArchiveReactant = reactant;
    this.isArchiveReactantModalOpen = true;
  }

  executeArchive() {
    if (this.pendingArchiveReactant) {
      const shortName = this.pendingArchiveReactant.shortName;
      this.reactantService.archiveReactant(this.pendingArchiveReactant.uuid).subscribe({
        next: (value) => {
          this.notification.success(
            this.transloco.translate('reactants.notifications.archivedTitle'),
            this.transloco.translate('reactants.notifications.archivedMessage', {
              name: shortName,
            }),
          );
          setTimeout(() => {
            this.cancelArchive();
            this.loadReactants();
          });
        },
        error: (err) => {
          this.notification.error(
            this.transloco.translate('reactants.notifications.archiveFailed'),
            err.message,
          );
        },
      });
    }
  }

  cancelRestore() {
    this.isRestoreReactantModalOpen = false;
    this.pendingRestoreReactant = undefined;
  }

  restoreReactant(reactant: ReactantDto | undefined): void {
    if (!reactant) return;

    this.pendingRestoreReactant = reactant;
    this.isRestoreReactantModalOpen = true;
  }

  executeRestore() {
    if (this.pendingRestoreReactant) {
      const shortName = this.pendingRestoreReactant.shortName;
      this.reactantService.restoreReactant(this.pendingRestoreReactant.uuid).subscribe({
        next: (value) => {
          this.notification.success(
            this.transloco.translate('reactants.notifications.restoredTitle'),
            this.transloco.translate('reactants.notifications.restoredMessage', {
              name: shortName,
            }),
          );
          setTimeout(() => {
            this.cancelRestore();
            this.loadReactants();
          });
        },
        error: (err) => {
          this.notification.error(
            this.transloco.translate('reactants.notifications.restoreFailed'),
            err.message,
          );
        },
      });
    }
  }

  copyToClipboard(text: string): void {
    if (navigator.clipboard) {
      navigator.clipboard
        .writeText(text)
        .then(() => {
          this.notification.success(
            this.transloco.translate('reactants.notifications.copySuccessTitle'),
            this.transloco.translate('reactants.notifications.copySuccessMessage'),
          );
        })
        .catch((err) => {
          this.notification.error(
            this.transloco.translate('reactants.notifications.copyFailed'),
            err.message || err.error || err,
          );
        });
    } else {
      const textArea = document.createElement('textarea');
      textArea.value = text;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    }
  }

  openDraw() {
    //TODO: add RDKit check if current Search is SMILES
    this.currentSmilesForModal = this.searchTerm;
    this.isDrawModalOpen = true;
  }

  onModalSave(newSmiles: string) {
    this.searchTerm = newSmiles;
    this.onSearch();
    this.isDrawModalOpen = false;
  }
}
