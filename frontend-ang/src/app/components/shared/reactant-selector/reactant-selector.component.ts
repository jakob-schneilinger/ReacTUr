import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DrawMolComponent } from '../draw-mol/draw-mol.component';
import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { ReactantDto } from '../../../dtos/reactant.dto';

@Component({
  selector: 'app-reactant-selector',
  templateUrl: './reactant-selector.component.html',
  styleUrls: ['./reactant-selector.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, DrawMolComponent],
})
export class ReactantSelectorComponent implements OnInit {
  @Input() label: string = 'Reactant';
  @Input() availableReactants: ReactantDto[] = [];

  @Input() set initialValue(val: string) {
    if (val) this.initReactant(val);
  }

  @Input() set clearSignal(val: number) {
    if (val) this.clearReactant();
  }

  @Output() selectionChanged = new EventEmitter<string>();

  searchTerm: string = '';
  selectedReactant: ReactantDto | null = null;
  suggestions: ReactantDto[] = [];

  isDrawModalOpen = false;
  currentSmilesForModal = '';

  ngOnInit() {}

  initReactant(val: string) {
    const match = this.availableReactants.find(r => r.shortName.toLowerCase() === val.toLowerCase());
    if (match) {
      this.selectedReactant = match;
    } else {
      this.searchTerm = val;
    }
  }

  onSearchInput() {
    if (this.searchTerm.length > 0) {
      const lowerTerm = this.searchTerm.toLowerCase();
      this.suggestions = this.availableReactants.filter(
        (r) =>
          r.shortName.toLowerCase().includes(lowerTerm) ||
          r.chemicalType.name.toLowerCase().includes(lowerTerm) ||
          r.chemicalName?.toLowerCase().includes(lowerTerm) ||
          r.smiles.includes(this.searchTerm),
      );
    } else {
      this.suggestions = [];
    }
  }

  selectReactant(reactant: ReactantDto) {
    this.selectedReactant = reactant;
    this.searchTerm = '';
    this.suggestions = [];
    this.emitChange();
  }

  clearReactant() {
    this.selectedReactant = null;
    this.searchTerm = '';
    this.suggestions = [];
    this.emitChange();
  }

  emitChange() {
    const value = this.selectedReactant ? this.selectedReactant.uuid : this.searchTerm;
    this.selectionChanged.emit(value);
  }

  openDraw() {
    this.currentSmilesForModal = this.selectedReactant ? this.selectedReactant.smiles : this.searchTerm;
    this.isDrawModalOpen = true;
  }

  onModalSave(newSmiles: string) {
    const matched = this.availableReactants.find(r => r.smiles === newSmiles);

    if (matched) {
      this.selectReactant(matched);
    } else {
      this.searchTerm = newSmiles;
      this.selectedReactant = null;
      this.onSearchInput();
    }
    this.isDrawModalOpen = false;
  }
}
