import { Component, Output, EventEmitter, Input, OnInit } from '@angular/core';
import { Reactant } from '../../dtos/reactant';

@Component({
  selector: 'app-experiment-search',
  templateUrl: './experiment-search.component.html',
  standalone: false,
})
export class ExperimentSearchComponent implements OnInit {
  @Output() searchChanged = new EventEmitter<{ reactant1: string; reactant2: string }>();

  triggerClearValue: number = 0;
  @Input() set triggerClear(value: number) {
    if (value) {
      this.triggerClearValue = value; // Wird an die Childs weitergegeben
      this.currentReactant1 = '';
      this.currentReactant2 = '';
    }
  }

  initVal1 = '';
  initVal2 = '';
  @Input() set reactant1(val: string) { if(val) this.initVal1 = val; }
  @Input() set reactant2(val: string) { if(val) this.initVal2 = val; }

  // State halten für das finale Emit
  currentReactant1 = '';
  currentReactant2 = '';

  availableReactants: Reactant[] = [
    { shortName: 'Sodium', smiles: '[Na]', type: 'Metal' },
    { shortName: 'Chlorine', smiles: 'Cl[Cl]', type: 'Halogen' },
    { shortName: 'Hydrogen', smiles: '[H][H]', type: 'Gas' },
    { shortName: 'Oxygen', smiles: 'O=O', type: 'Gas' },
    { shortName: 'Ethanol', smiles: 'CCO', type: 'Solvent' }
  ];

  ngOnInit() {
    this.currentReactant1 = this.initVal1;
    this.currentReactant2 = this.initVal2;
  }

  onReactantChange(num: 1 | 2, value: string) {
    if (num === 1) {
      this.currentReactant1 = value;
    } else {
      this.currentReactant2 = value;
    }

    this.searchChanged.emit({
      reactant1: this.currentReactant1,
      reactant2: this.currentReactant2,
    });
  }
}
