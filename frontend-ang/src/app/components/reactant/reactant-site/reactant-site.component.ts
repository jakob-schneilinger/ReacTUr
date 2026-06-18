import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ReactantDto } from '../../../dtos/reactant.dto';
import { ReactantService } from '../../../services/reactant.service';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-reactant-site',
  templateUrl: './reactant-site.component.html',
  styleUrls: ['./reactant-site.component.scss'],
  standalone: false,
})
export class ReactantSiteComponent implements OnInit {
  reactants: ReactantDto[] = [];
  filteredReactants: ReactantDto[] = [];
  searchTerm: string = '';

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
    this.reactantService.getAllReactants().subscribe({
      next: (value) => {
        this.reactants = value;
        this.onSearch();
        this.cdr.detectChanges();
      },
      error: (err) =>
        this.notification.error('Reactants unavailable', err.message || err.error || err),
    });

    this.role = this.authService.getUserRole();
    this.hasWritePermission = this.role != 'USER';
  }

  onSearch(): void {
    const term = this.searchTerm.toLowerCase().trim();
    if (!term) {
      this.filteredReactants = [...this.reactants];
      return;
    }

    this.filteredReactants = this.reactants.filter(
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

    console.log(uuid);

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
    //TODO: call archive
    if (this.pendingReactant) {
      const shortName = this.pendingReactant.uuid;
      this.reactants = this.reactants.filter((r) => r.uuid !== this.pendingReactant?.uuid);
      this.onSearch();
      this.notification.success(
        'Reactant Archived',
        `The reactant "${shortName}" was successfully archived.`,
      );
    }
    this.cancelArchive();
  }

  archiveReactant(reactant: ReactantDto | undefined): void {
    if (!reactant) return;

    this.pendingReactant = reactant;
    this.isArchiveReactantModalOpen = true;
  }

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
    this.currentSmilesForModal = '';
    this.isDrawModalOpen = true;
  }

  onModalSave(newSmiles: string) {
    this.searchTerm = newSmiles;
    this.onSearch();
    this.isDrawModalOpen = false;
  }
}
