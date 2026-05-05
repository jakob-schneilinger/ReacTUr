import { Component, Output, EventEmitter, Input } from '@angular/core';

@Component({
  selector: 'app-experiment-search',
  templateUrl: './experiment-search.component.html',
  styleUrls: ['./experiment-search.component.scss'],
  standalone: false,
})
export class ExperimentSearchComponent {
  // Event, das ausgelöst wird, wenn sich die Suche ändert
  @Output() searchChanged = new EventEmitter<{ reactant1: string; reactant2: string }>();
  @Input() set triggerClear(value: number) {
    if (value) {
      this.clearReactant1();
      this.clearReactant2();
    }
  }

  reactant1: string = '';
  reactant2: string = '';

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
}
