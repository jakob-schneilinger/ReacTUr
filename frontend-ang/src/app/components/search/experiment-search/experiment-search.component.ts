import { Component, Output, EventEmitter, Input } from '@angular/core';
import { ReactantDto } from '../../../dtos/reactant.dto';
import { MOCK_REACTANTS } from '../../../mock/mock-experiments';

@Component({
  selector: 'app-experiment-search',
  templateUrl: './experiment-search.component.html',
  styleUrls: ['./experiment-search.component.scss'],
  standalone: false,
})
export class ExperimentSearchComponent {
  @Output() searchChanged = new EventEmitter<{
    reactant1: string[];
    reactant2: string[];
  }>();

  triggerClearValue: number = 0;

  @Input() set triggerClear(value: number) {
    if (value) {
      this.triggerClearValue = value;
      this.currentExact1 = '';
      this.currentExact2 = '';
      this.currentSub1 = [];
      this.currentSub2 = [];
      this.showSub1 = false;
      this.showSub2 = false;
    }
  }

  initVal1: string[] = [];
  initVal2: string[] = [];

  @Input() set reactant1(val: string[]) {
    if (val) this.initVal1 = val;
  }
  @Input() set reactant2(val: string[]) {
    if (val) this.initVal2 = val;
  }

  showSub1 = false;
  showSub2 = false;

  currentExact1 = '';
  currentExact2 = '';
  currentSub1: string[] = [];
  currentSub2: string[] = [];

  availableReactants: ReactantDto[] = MOCK_REACTANTS; //TODO: do service call

  onReactantChange(num: 1 | 2, value: string) {
    if (num === 1) this.currentExact1 = value;
    else this.currentExact2 = value;
    this.emitSearch();
  }

  onSubstructureMatch(num: 1 | 2, matchedNames: string[]) {
    if (num === 1) this.currentSub1 = matchedNames;
    else this.currentSub2 = matchedNames;
    this.emitSearch();
  }

  emitSearch() {
    // Wenn Substruktur aktiv und gefüllt -> nimm Array.
    // Ansonsten nimm Exact -> wickle ihn in ein Array (oder leeres Array falls leer).
    const r1 =
      this.showSub1 && this.currentSub1.length > 0
        ? this.currentSub1
        : this.currentExact1
          ? [this.currentExact1]
          : [];

    const r2 =
      this.showSub2 && this.currentSub2.length > 0
        ? this.currentSub2
        : this.currentExact2
          ? [this.currentExact2]
          : [];

    this.searchChanged.emit({
      reactant1: r1,
      reactant2: r2,
    });
  }
}
