import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DrawMolComponent } from '../../../../shared/draw-mol/draw-mol.component';
import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { ReactantDto } from '../../../reactants/reactant.dto';

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
  @Input() reactantLimit: number = 0;
  @Input() zIndex: number = 0;

  @Input() set initialValue(vals: string[]) {
    if (vals && vals.length > 0) this.initReactants(vals);
  }

  @Input() set clearSignal(val: number) {
    if (val) this.clearAll();
  }

  @Output() selectionChanged = new EventEmitter<string[]>();

  searchTerm: string = '';
  selectedReactants: ReactantDto[] = [];
  suggestions: ReactantDto[] = [];

  isDrawModalOpen = false;
  currentSmilesForModal = '';

  get isLimitReached(): boolean {
    return this.reactantLimit > 0 && this.selectedReactants.length >= this.reactantLimit;
  }

  ngOnInit() {}

  initReactants(vals: string[]) {
    console.log('initReactants', vals);
    this.selectedReactants = [];
    let matchFound = false;
    vals.forEach((val) => {
      const match = this.availableReactants.find(
        (r) => r.shortName.toLowerCase() === val.toLowerCase() || r.uuid === val,
      );
      if (match && !this.selectedReactants.includes(match)) {
        this.selectedReactants.push(match);
        matchFound = true;
      }
    });
    console.log('matchFound', matchFound);

    if (matchFound) {
      this.emitChange();
    } else {
      if (vals.length === 1) {
        this.searchTerm = vals[0];
        this.onSearchInput();
      }
    }
  }

  onSearchInput() {
    if (this.searchTerm.length > 0) {
      const lowerTerm = this.searchTerm.toLowerCase();
      this.suggestions = this.availableReactants.filter(
        (r) =>
          !this.selectedReactants.some((selected) => selected.uuid === r.uuid) &&
          (r.shortName.toLowerCase().includes(lowerTerm) ||
            r.chemicalType?.name.toLowerCase().includes(lowerTerm) ||
            r.chemicalName?.toLowerCase().includes(lowerTerm) ||
            r.smiles.includes(this.searchTerm)),
      );
    } else {
      this.suggestions = [];
    }
  }

  selectReactant(reactant: ReactantDto) {
    if (this.isLimitReached) {
      return;
    }
    if (!this.selectedReactants.find((r) => r.uuid === reactant.uuid)) {
      this.selectedReactants.push(reactant);
    }
    this.clearSearch();
    this.emitChange();
  }

  removeReactant(reactant: ReactantDto) {
    this.selectedReactants = this.selectedReactants.filter((r) => r.uuid !== reactant.uuid);
    this.emitChange();
  }

  clearAll() {
    this.selectedReactants = [];
    this.clearSearch();
    this.emitChange();
  }

  clearSearch() {
    this.searchTerm = '';
    this.suggestions = [];
  }

  emitChange() {
    const values = this.selectedReactants.map((r) => r.uuid);
    this.selectionChanged.emit(values);
  }

  openDraw() {
    this.currentSmilesForModal = this.searchTerm;
    this.isDrawModalOpen = true;
  }

  onModalSave(newSmiles: string) {
    const matched = this.availableReactants.find((r) => r.smiles === newSmiles);

    if (matched) {
      this.selectReactant(matched);
    } else {
      this.searchTerm = newSmiles;
      this.onSearchInput();
    }
    this.isDrawModalOpen = false;
  }
}
