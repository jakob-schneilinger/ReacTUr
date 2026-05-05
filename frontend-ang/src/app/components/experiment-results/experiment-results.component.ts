import { Component, Input, Output, EventEmitter } from '@angular/core';

export type SearchState = 'initial' | 'no-results' | 'has-results';

// TODO: add to dto when structure fixed
export interface Experiment {
  id: number;
  reactant1: string;
  reactant2: string;
  dateYear?: string;
  dateMonth?: string;
  dateDay?: string;
  solvent?: string;
  scientist?: string;
  temp?: number;
  concentrationA?: string;
  concentrationB?: string;
  instrument?: string;
  modality?: string;
  wavelength?: string;
  comment?: string;
}

@Component({
  selector: 'app-experiment-results',
  templateUrl: './experiment-results.component.html',
  styleUrls: ['./experiment-results.component.scss'],
  standalone: false,
})
export class ExperimentResultsComponent {
  @Input() state: SearchState = 'initial';
  @Input() experiments: Experiment[] = []; // Ersetzen Sie 'any' mit Ihrem echten Interface
  @Output() createNewReaction = new EventEmitter<void>();
}
