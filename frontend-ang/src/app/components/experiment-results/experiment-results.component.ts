import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Experiment } from '../../dtos/experiment';

export type SearchState = 'initial' | 'no-results' | 'has-results';


@Component({
  selector: 'app-experiment-results',
  templateUrl: './experiment-results.component.html',
  styleUrls: ['./experiment-results.component.scss'],
  standalone: false,
})
export class ExperimentResultsComponent {
  @Input() state: SearchState = 'initial';
  @Input() experiments: Experiment[] = [];
  @Output() getExport = new EventEmitter<number[]>();

  selectedIds = new Set<number>();

  toggleSelection(expId: number) {
    if (this.selectedIds.has(expId)) {
      this.selectedIds.delete(expId);
    } else {
      this.selectedIds.add(expId);
    }
  }

  isSelected(expId: number): boolean {
    return this.selectedIds.has(expId);
  }

  get allSelected(): boolean {
    if (this.experiments.length === 0) return false;
    return this.experiments.every(exp => this.selectedIds.has(exp.id));
  }

  toggleSelectAll() {
    if (this.allSelected) {
      this.experiments.forEach(exp => this.selectedIds.delete(exp.id));
    } else {
      this.experiments.forEach(exp => this.selectedIds.add(exp.id));
    }
  }

  onExportClicked() {
    this.getExport.emit(Array.from(this.selectedIds));
  }
}
