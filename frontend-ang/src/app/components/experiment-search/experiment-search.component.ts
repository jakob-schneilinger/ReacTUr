import { Component, Output, EventEmitter, Input } from '@angular/core';

@Component({
  selector: 'app-experiment-search',
  templateUrl: './experiment-search.component.html',
  styleUrls: ['./experiment-search.component.scss'],
  standalone: false,
})
export class ExperimentSearchComponent {
  @Output() searchChanged = new EventEmitter<{ reactant1: string; reactant2: string }>();
  @Input() set triggerClear(value: number) {
    if (value) {
      this.clearReactant1();
      this.clearReactant2();
    }
  }
  @Input() reactant1: string = "";
  @Input() reactant2: string = "";

  isDrawModalOpen = false;
  activeReactant: 1 | 2 = 1;
  currentSmilesForModal = '';

  onSearch() {
    this.searchChanged.emit({
      reactant1: this.reactant1,
      reactant2: this.reactant2,
    });
  }

  clearReactant1() {
    this.reactant1 = '';
    this.onSearch();
  }

  clearReactant2() {
    this.reactant2 = '';
    this.onSearch();
  }

  openDraw(reactantNum: 1 | 2) {
    this.activeReactant = reactantNum;
    this.currentSmilesForModal = reactantNum === 1 ? this.reactant1 : this.reactant2;
    this.isDrawModalOpen = true;
  }

  onModalSave(newSmiles: string) {
    if (this.activeReactant === 1) {
      this.reactant1 = newSmiles;
    } else {
      this.reactant2 = newSmiles;
    }
    this.isDrawModalOpen = false;
    this.onSearch();
  }
}
