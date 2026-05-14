import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Reactant } from '../../../dtos/reactant';
import { ReactantService } from '../../../services/reactant.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-reactant-site',
  templateUrl: './reactant-site.component.html',
  styleUrls: ['./reactant-site.component.scss'],
  standalone: false,
})
export class ReactantSiteComponent implements OnInit {
  reactants: Reactant[] = [];
  filteredReactants: Reactant[] = [];
  searchTerm: string = '';

  isDeleteReactantModalOpen: boolean = false;
  pendingReactantName: string | null = '';

  isViewModalOpen: boolean = false;
  selectedReactantForView: Reactant | undefined = undefined;

  constructor(
    private router: Router,
    private reactantService: ReactantService,
    private notification: NotificationService,
  ) {}

  ngOnInit(): void {
    this.reactantService.getAllReactants().subscribe({
      next: (value) => {
        this.reactants = value;
        this.onSearch();
      },
      error: (err) => this.notification.error('Reactants unavailable', err),
    });
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
        reactant.type.toLowerCase().includes(term),
    );
  }

  goToCreate(): void {
    this.router.navigate(['/reactants/create']);
  }

  viewDetails(shortName: string | undefined): void {
    if (!shortName) return;

    const reactantToView = this.reactants.find((r) => r.shortName === shortName);

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

  editReactant(shortName: string | undefined): void {
    if (shortName) this.router.navigate(['/reactants', shortName]);
  }

  cancelDelete() {
    this.isDeleteReactantModalOpen = false;
    this.pendingReactantName = null;
  }

  executeDelete() {
    if (this.pendingReactantName !== null) {
      const tempName = this.pendingReactantName;
      this.reactants = this.reactants.filter((r) => r.shortName !== this.pendingReactantName);
      this.onSearch();
      this.notification.success(
        'Reactant Removed',
        `The reactant "${tempName}" was successfully removed.`,
      );
    }
    this.cancelDelete();
  }

  deleteReactant(shortName: string | undefined): void {
    if (!shortName) return;

    this.pendingReactantName = shortName;
    this.isDeleteReactantModalOpen = true;
  }

  copyToClipboard(text: string): void {
    if (navigator.clipboard) {
      navigator.clipboard
        .writeText(text)
        .then(() => {
          this.notification.success('Success', 'SMILES copied to clipboard!');
        })
        .catch((err) => {
          this.notification.error('Failed to copy text', err);
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
}
