import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchInputComponent } from '../../shared/search-input/search-input.component';
import { ReactantCardComponent } from '../reactant-card/reactant-card.component';
import { AppConfirmModalComponent } from '../../shared/modal/confirm-modal.component';
import { ReactantViewModalComponent } from '../reactant-view-modal/reactant-view-modal.component';
import { DrawMolComponent } from '../../shared/draw-mol/draw-mol.component';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ReactantDto } from '../../../dtos/reactant.dto';
import { ReactantService } from '../../../services/reactant.service';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth.service';
import { HasRoleDirective } from '../../../directives/has-role.directive';

@Component({
  selector: 'app-reactant-site',
  templateUrl: './reactant-site.component.html',
  styleUrls: ['./reactant-site.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SearchInputComponent,
    ReactantCardComponent,
    AppConfirmModalComponent,
    ReactantViewModalComponent,
    DrawMolComponent,
    HasRoleDirective,
  ],
})
export class ReactantSiteComponent implements OnInit {
  reactants: ReactantDto[] = [];
  filteredReactants: ReactantDto[] = [];
  searchTerm: string = '';

  archivedReactants: ReactantDto[] = [];
  showArchived: boolean = false;

  activeReactants: ReactantDto[] = [];

  role: string = 'USER';
  hasWritePermission: boolean = false;

  isArchiveReactantModalOpen: boolean = false;
  pendingReactant: ReactantDto | undefined = undefined;

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
  ) {}

  ngOnInit(): void {
    this.role = this.authService.getUserRole();
    this.hasWritePermission = this.role != 'USER';
    this.loadReactants();
  }

  private loadReactants() {
    this.reactantService.getAllReactants().subscribe({
      next: (value) => {
        this.reactants = value;
        if (!this.showArchived) {
          this.activeReactants = this.reactants;
          this.onSearch();
          this.cdr.markForCheck();
        }
      },
      error: (err) =>
        this.notification.error('Reactants unavailable', err.message || err.error || err),
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
            'Archived Reactants unavailable',
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
        reactant.chemicalType.name.toLowerCase().includes(term),
    );
  }

  goToCreate(): void {
    this.router.navigate(['/reactants/create']);
  }

  viewDetails(uuid: string | undefined): void {
    if (!uuid) return;

    const reactantToView = this.reactants.find((r) => r.uuid === uuid);

    if (reactantToView) {
      this.selectedReactantForView = reactantToView;
      this.isViewModalOpen = true;
    } else {
      this.notification.error('Error', 'Reactant details could not be found.');
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
    this.pendingReactant = undefined;
  }

  executeArchive() {
    if (this.pendingReactant) {
      const shortName = this.pendingReactant.shortName;
      this.reactantService.archiveReactant(this.pendingReactant.uuid).subscribe({
        next: (value) => {
          this.notification.success(
            'Reactant Archived',
            `The reactant "${shortName}" was successfully archived.`,
          );
          setTimeout(() => {
            this.cancelArchive();
            this.loadReactants();
          });
        },
        error: (err) => {
          this.notification.error('Archiving Failed', err.message);
        },
      });
    }
  }

  archiveReactant(reactant: ReactantDto | undefined): void {
    if (!reactant) return;

    this.pendingReactant = reactant;
    this.isArchiveReactantModalOpen = true;
  }

  restoreReactant(reactant: ReactantDto | undefined): void {
    //TODO: think about restore Modal. is it needed?
    if (reactant) {
      const shortName = reactant.shortName;
      this.reactantService.restoreReactant(reactant.uuid).subscribe({
        next: (value) => {
          this.notification.success(
            'Reactant Restored',
            `The reactant "${shortName}" was successfully restored.`,
          );
          setTimeout(() => {
            this.loadReactants();
          });
        },
        error: (err) => {
          this.notification.error('Restoring Failed', err.message);
        },
      });
    }
  };

  copyToClipboard(text: string): void {
    if (navigator.clipboard) {
      navigator.clipboard
        .writeText(text)
        .then(() => {
          this.notification.success('Success', 'SMILES copied to clipboard!');
        })
        .catch((err) => {
          this.notification.error('Failed to copy text', err.message || err.error || err);
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
